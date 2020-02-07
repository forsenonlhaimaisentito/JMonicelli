/*
 * Copyright (c) 2020 forsenonlhaimaisentito
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.catafratta.jmonicelli.compiler.impl

import org.catafratta.jmonicelli.ast.*
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.GeneratorAdapter
import org.objectweb.asm.commons.Method
import java.io.InputStream
import java.io.PrintStream
import java.util.*

class ModuleGenerator(
    val module: MonicelliModule,
    private val className: String,
    private val cv: ClassVisitor
) {
    private val thisType = Type.getObjectType(className)

    fun generateClass() {
        cv.visit(
            Opcodes.V1_8,
            CLASS_ACCESS,
            className,
            null,
            "java/lang/Object",
            emptyArray()
        )

        generateStaticFields()
        generateStaticInitializer()
        generateJavaMain()

        module.functions.forEach { generateFunction(it) }
        module.mainFunction?.let { generateMainFunction(it) }

        cv.visitEnd()
    }

    private fun generateStaticFields() {
        cv.visitField(
            Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC,
            SCANNER_FIELD,
            SCANNER_TYPE.descriptor,
            null,
            null
        )
    }

    private fun generateStaticInitializer() {
        val generator = GeneratorAdapter(
            Opcodes.ACC_STATIC,
            Method("<clinit>", "()V"),
            null, null, cv
        )

        generator.apply {
            visitCode()
            newInstance(SCANNER_TYPE)
            dup()
            getStatic(SYSTEM_TYPE, "in", INPUTSTREAM_TYPE)
            invokeConstructor(SCANNER_TYPE, Method("<init>", Type.VOID_TYPE, arrayOf(INPUTSTREAM_TYPE)))
            putStatic(thisType, SCANNER_FIELD, SCANNER_TYPE)
            returnValue()

            //visitMaxs(0, 0)
            endMethod()
        }
    }

    private fun generateJavaMain() {
        GeneratorAdapter(MAIN_ACCESS, JAVA_MAIN_METHOD, null, null, cv).apply {
            visitCode()

            invokeStatic(thisType, MAIN_METHOD)
            cast(Type.LONG_TYPE, Type.INT_TYPE)
            invokeStatic(SYSTEM_TYPE, Method("exit", "(I)V"))
            returnValue()

            //visitMaxs(0, 0)
            endMethod()
        }
    }

    private fun generateMainFunction(function: MonicelliFunction) {
        GeneratorAdapter(MAIN_ACCESS, MAIN_METHOD, null, null, cv)
            .generateFunctionBody(function)
    }

    private fun generateFunction(function: MonicelliFunction) {
        GeneratorAdapter(METHOD_ACCESS, function.toAsmMethod(), null, null, cv)
            .generateFunctionBody(function)
    }

    private fun GeneratorAdapter.generateFunctionBody(function: MonicelliFunction) {
        val context = MethodContext.fromFunction(this, function)
        function.params.forEach { param ->
            visitParameter(param.name, 0)
        }
        visitCode()
        function.body.forEach { context.generateStatement(it) }

        // Just to be safe
        pushDefaultValue(function.returnType)
        returnValue()

        // visitMaxs(-1, -1)
        endMethod()
    }

    private fun MethodContext.generateStatement(statement: MonicelliStatement) {
        when (statement) {
            is MonicelliStatement.Read -> generateStatement(statement)
            is MonicelliStatement.Print -> generateStatement(statement)
            is MonicelliStatement.Variable -> generateStatement(statement)
            is MonicelliStatement.Assignment -> generateStatement(statement)
            is MonicelliStatement.Return -> generateStatement(statement)
            is MonicelliStatement.Call -> generateStatement(statement)
            is MonicelliStatement.Assert -> generateStatement(statement)
            is MonicelliStatement.Abort -> generateStatement(statement)
            is MonicelliStatement.Loop -> generateStatement(statement)
            is MonicelliStatement.Branch -> generateStatement(statement)
        }
    }

    private fun MethodContext.generateStatement(statement: MonicelliStatement.Read) {
        val (dest, isLocal) = findVariable(statement.target)

        val scannerMethod = when (dest.type) {
            MonicelliType.INT -> Method("nextLong", "()J")
            MonicelliType.CHAR -> Method("nextChar", "()S")
            MonicelliType.FLOAT -> Method("nextFloat", "()F")
            MonicelliType.BOOL -> Method("nextBoolean", "()Z")
            MonicelliType.DOUBLE -> Method("nextDouble", "()D")
            MonicelliType.VOID -> throw IllegalStateException("Can't read to void")
        }

        generator.getStatic(thisType, SCANNER_FIELD, SCANNER_TYPE)
        generator.invokeVirtual(SCANNER_TYPE, scannerMethod)
        if (isLocal) generator.storeLocal(dest.index, dest.asmType) else generator.storeArg(dest.index)
    }

    private fun MethodContext.generateStatement(statement: MonicelliStatement.Print) {
        val type = resolveExpressionType(statement.expression)
        if (type == MonicelliType.VOID)
            throw IllegalStateException("Can't print void values")

        generator.getStatic(SYSTEM_TYPE, "out", PRINTSTREAM_TYPE)
        generateExpression(statement.expression)
        generator.invokeVirtual(PRINTSTREAM_TYPE, Method("println", Type.VOID_TYPE, arrayOf(type.toAsmType())))
    }

    private fun MethodContext.generateStatement(statement: MonicelliStatement.Variable) {
        if (hasVariable(statement.name)) throw IllegalStateException("Redefinition of variable ${statement.name}")

        val index = generator.newLocal(statement.type.toAsmType())
        statement.value?.let { expr ->
            generateExpression(expr, statement.type)
        } ?: generator.pushDefaultValue(statement.type)
        // TODO: Initial value for variables is undefined in the Monicelli 2.0 spec, so inserting a call to Random
        //       won't violate compatibility while still providing RNG and add to the supercazzola feel

        generator.storeLocal(index)

        locals[statement.name] = Var(statement.name, statement.type, statement.type.toAsmType(), index)
    }

    private fun MethodContext.generateStatement(statement: MonicelliStatement.Assignment) {
        val (dest, isLocal) = findVariable(statement.target)

        generateExpression(statement.value, dest.type)
        if (isLocal) generator.storeLocal(dest.index, dest.asmType) else generator.storeArg(dest.index)
    }

    private fun MethodContext.generateStatement(statement: MonicelliStatement.Return) {
        if (statement.value == null) {
            generator.push(0)  // Just to be safe
        } else {
            generateExpression(statement.value, function.returnType)
        }

        generator.returnValue()
    }

    private fun MethodContext.generateStatement(statement: MonicelliStatement.Call) {
        val func = module.functions.find { it.name == statement.target }
            ?: throw IllegalStateException("Undefined function ${statement.target}")

        statement.args.forEachIndexed { i, arg -> generateExpression(arg, func.params[i].type) }
        generator.invokeStatic(thisType, func.toAsmMethod())
        when (func.returnType) {
            MonicelliType.INT, MonicelliType.DOUBLE -> generator.pop2()
            MonicelliType.VOID -> Unit
            else -> generator.pop()
        }
    }

    private fun MethodContext.generateStatement(statement: MonicelliStatement.Assert) {
        val successLabel = generator.newLabel()

        generateIntExpression(statement.expression)
        generator.ifZCmp(GeneratorAdapter.NE, successLabel)

        generateErrorMessage("Assertion failed at ${statement.source}")
        generator.push(-1)
        generator.invokeStatic(SYSTEM_TYPE, Method("exit", "(I)V"))

        // Actually unreachable
        generator.pushDefaultValue(function.returnType)
        generator.returnValue()

        generator.mark(successLabel)
    }

    private fun MethodContext.generateStatement(statement: MonicelliStatement.Abort) {
        generateErrorMessage("Program aborted at ${statement.source}")
        generator.push(-1)
        generator.invokeStatic(SYSTEM_TYPE, Method("exit", "(I)V"))
    }

    private fun MethodContext.generateStatement(statement: MonicelliStatement.Loop) {
        val startLabel = generator.mark()
        statement.body.forEach { generateStatement(it) }

        generateIntExpression(statement.condition)
        generator.ifZCmp(GeneratorAdapter.NE, startLabel)
    }

    private fun MethodContext.generateStatement(statement: MonicelliStatement.Branch) {
        // val (target, isLocal) = findVariable(statement.target)
        val pathsLabels = statement.paths.map { generator.newLabel() }
        val defaultLabel = generator.newLabel()
        val endLabel = generator.newLabel()

        statement.paths.forEachIndexed { i, (cond, _) ->
            generateIntExpression(cond)
            generator.ifZCmp(GeneratorAdapter.NE, pathsLabels[i])
        }

        if (statement.defaultPath != null) generator.goTo(defaultLabel)
        generator.goTo(endLabel)

        statement.paths.forEachIndexed { i, (_, body) ->
            generator.mark(pathsLabels[i])
            body.forEach { generateStatement(it) }
            generator.goTo(endLabel)
        }

        statement.defaultPath?.let { body ->
            generator.mark(defaultLabel)
            body.forEach { generateStatement(it) }
            generator.goTo(endLabel)
        }

        generator.mark(endLabel)
    }

    private fun MethodContext.generateIntExpression(expression: MonicelliExpression) {
        val conditionType = resolveExpressionType(expression)
        generateExpression(expression)
        generator.cast(conditionType.toAsmType(), Type.INT_TYPE)
    }

    private fun MethodContext.generateErrorMessage(message: String) {
        generator.getStatic(SYSTEM_TYPE, "err", PRINTSTREAM_TYPE)
        generator.push(message)
        generator.invokeVirtual(PRINTSTREAM_TYPE, Method("println", "(Ljava/lang/String;)V"))
    }

    private fun MethodContext.hasVariable(name: String): Boolean {
        return name in locals || name in params
    }

    private fun MethodContext.findVariable(name: String): Pair<Var, Boolean> {
        var isLocal = false
        val dest = locals[name]?.also { isLocal = true }
            ?: params[name]
            ?: throw IllegalStateException("Undefined variable $name")

        return dest to isLocal
    }

    private fun MethodContext.generateExpression(expression: MonicelliExpression, expectedType: MonicelliType? = null) {
        val actualType = resolveExpressionType(expression)

        when (expression) {
            is MonicelliExpression.Call -> generateExpression(expression)
            is MonicelliExpression.Mult -> generateExpression(expression)
            is MonicelliExpression.Div -> generateExpression(expression)
            is MonicelliExpression.Plus -> generateExpression(expression)
            is MonicelliExpression.Minus -> generateExpression(expression)
            is MonicelliExpression.Shift -> generateExpression(expression)
            is MonicelliExpression.Comparison -> generateExpression(expression)
            is MonicelliExpression.Identifier -> generateExpression(expression)
            is MonicelliExpression.IntImmediate -> generateExpression(expression)
            is MonicelliExpression.FloatImmediate -> generateExpression(expression)
        }

        if (expectedType != null && actualType != expectedType) generateCast(actualType, expectedType)
    }

    private fun MethodContext.generateCast(actual: MonicelliType, expected: MonicelliType) {
        generator.cast(actual.toAsmType(), expected.toAsmType())
    }

    private fun MethodContext.generateExpression(expression: MonicelliExpression.Call) {
        val func = module.functions.find { it.name == expression.target }
            ?: throw IllegalStateException("Undefined function ${expression.target}")

        expression.args.forEachIndexed { i, arg -> generateExpression(arg, func.params[i].type) }
        generator.invokeStatic(thisType, func.toAsmMethod())
    }

    private fun MethodContext.generateExpression(expression: MonicelliExpression.Mult) {
        val resultType = resolveExpressionType(expression)
        generateExpression(expression.lhs, resultType)
        generateExpression(expression.rhs, resultType)
        generator.math(GeneratorAdapter.MUL, resultType.toAsmType())
    }

    private fun MethodContext.generateExpression(expression: MonicelliExpression.Div) {
        val resultType = resolveExpressionType(expression)
        generateExpression(expression.lhs, resultType)
        generateExpression(expression.rhs, resultType)
        generator.math(GeneratorAdapter.DIV, resultType.toAsmType())
    }

    private fun MethodContext.generateExpression(expression: MonicelliExpression.Plus) {
        val resultType = resolveExpressionType(expression)
        generateExpression(expression.lhs, resultType)
        generateExpression(expression.rhs, resultType)
        generator.math(GeneratorAdapter.ADD, resultType.toAsmType())
    }

    private fun MethodContext.generateExpression(expression: MonicelliExpression.Minus) {
        val resultType = resolveExpressionType(expression)
        generateExpression(expression.lhs, resultType)
        generateExpression(expression.rhs, resultType)
        generator.math(GeneratorAdapter.SUB, resultType.toAsmType())
    }

    private fun MethodContext.generateExpression(expression: MonicelliExpression.Shift) {
        val oper = when (expression.direction) {
            MonicelliExpression.Shift.Direction.LEFT -> GeneratorAdapter.SHL
            MonicelliExpression.Shift.Direction.RIGHT -> GeneratorAdapter.USHR
        }

        generateExpression(expression.lhs, MonicelliType.INT)
        generator.math(oper, MonicelliType.INT.toAsmType())
    }

    private fun MethodContext.generateExpression(expression: MonicelliExpression.Comparison) {
        val trueLabel = generator.newLabel()
        val endLabel = generator.newLabel()
        val commonType = binaryOpType(resolveExpressionType(expression.lhs), resolveExpressionType(expression.rhs))

        generateExpression(expression.lhs, commonType)
        generator.valueOf(commonType.toAsmType())
        generateExpression(expression.rhs, commonType)
        generator.valueOf(commonType.toAsmType())
        generator.invokeInterface(COMPARABLE_TYPE, COMPARE_TO)

        when (expression.operator) {
            MonicelliExpression.Comparison.Operator.LT -> generator.ifZCmp(GeneratorAdapter.LT, trueLabel)
            MonicelliExpression.Comparison.Operator.GT -> generator.ifZCmp(GeneratorAdapter.GT, trueLabel)
            MonicelliExpression.Comparison.Operator.LE -> generator.ifZCmp(GeneratorAdapter.LE, trueLabel)
            MonicelliExpression.Comparison.Operator.GE -> generator.ifZCmp(GeneratorAdapter.GE, trueLabel)
            MonicelliExpression.Comparison.Operator.EQ -> generator.ifZCmp(GeneratorAdapter.EQ, trueLabel)
        }

        generator.push(false)
        generator.goTo(endLabel)
        generator.mark(trueLabel)
        generator.push(true)
        generator.mark(endLabel)
    }

    private fun MethodContext.generateExpression(expression: MonicelliExpression.Identifier) {
        locals[expression.name]?.let { generator.loadLocal(it.index) }
            ?: params[expression.name]?.let { generator.loadArg(it.index) }
            ?: throw IllegalStateException("Undefined variable ${expression.name}")
    }

    private fun MethodContext.generateExpression(expression: MonicelliExpression.IntImmediate) {
        generator.push(expression.value)
    }

    private fun MethodContext.generateExpression(expression: MonicelliExpression.FloatImmediate) {
        generator.push(expression.value)
    }

    private fun MethodContext.resolveExpressionType(expression: MonicelliExpression): MonicelliType {
        return when (expression) {
            is MonicelliExpression.Call ->
                module.functions.find { it.name == expression.target }?.returnType
                    ?: throw IllegalStateException("Undefined function ${expression.target}")
            is MonicelliExpression.Mult ->
                binaryOpType(resolveExpressionType(expression.lhs), resolveExpressionType(expression.rhs))
            is MonicelliExpression.Div ->
                binaryOpType(resolveExpressionType(expression.lhs), resolveExpressionType(expression.rhs))
            is MonicelliExpression.Plus ->
                binaryOpType(resolveExpressionType(expression.lhs), resolveExpressionType(expression.rhs))
            is MonicelliExpression.Minus ->
                binaryOpType(resolveExpressionType(expression.lhs), resolveExpressionType(expression.rhs))
            is MonicelliExpression.Shift -> resolveExpressionType(expression.lhs)
            is MonicelliExpression.Comparison -> MonicelliType.BOOL
            is MonicelliExpression.Identifier ->
                locals[expression.name]?.type
                    ?: params[expression.name]?.type
                    ?: throw IllegalStateException("Undefined variable ${expression.name}")
            is MonicelliExpression.IntImmediate -> MonicelliType.INT
            is MonicelliExpression.FloatImmediate -> MonicelliType.DOUBLE
        }
    }

    private data class MethodContext(
        val generator: GeneratorAdapter,
        val function: MonicelliFunction,
        val params: Map<String, Var>,
        val locals: MutableMap<String, Var> = mutableMapOf()
    ) {
        companion object {
            fun fromFunction(generator: GeneratorAdapter, function: MonicelliFunction): MethodContext {
                val params = function.params.mapIndexed { i, param ->
                    param.name to Var(
                        param.name,
                        param.type,
                        param.type.toAsmType(),
                        i
                    )
                }.toMap()

                return MethodContext(generator, function, params)
            }
        }
    }

    private data class Var(val name: String, val type: MonicelliType, val asmType: Type, val index: Int)

    companion object {
        private const val CLASS_ACCESS = Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL
        private const val METHOD_ACCESS = Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC + Opcodes.ACC_FINAL
        private const val MAIN_ACCESS = Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC + Opcodes.ACC_FINAL
        private val MAIN_METHOD = Method("__mc_main", "()J")
        private val JAVA_MAIN_METHOD = Method("main", "([Ljava/lang/String;)V")

        private val COMPARABLE_TYPE = Type.getType(Comparable::class.java)
        private val COMPARE_TO = Method("compareTo", "(Ljava/lang/Object;)I")

        private val SYSTEM_TYPE = Type.getType(System::class.java)
        private val INPUTSTREAM_TYPE = Type.getType(InputStream::class.java)
        private val PRINTSTREAM_TYPE = Type.getType(PrintStream::class.java)
        private val SCANNER_TYPE = Type.getType(Scanner::class.java)

        private const val SCANNER_FIELD = "scanner"

        // See https://en.cppreference.com/w/c/language/conversion
        private fun binaryOpType(lhs: MonicelliType, rhs: MonicelliType): MonicelliType {
            val rank = listOf(
                MonicelliType.DOUBLE,
                MonicelliType.FLOAT,
                MonicelliType.INT,
                MonicelliType.CHAR,
                MonicelliType.BOOL
            )

            return when {
                lhs == rhs -> lhs
                rank.indexOf(lhs) < rank.indexOf(rhs) -> lhs
                else -> rhs
            }
        }

        private fun MonicelliFunction.toAsmMethod(): Method = Method(
            name,
            returnType.toAsmType(),
            params.map { it.type.toAsmType() }.toTypedArray()
        )

        private fun MonicelliType.toAsmType(): Type = when (this) {
            MonicelliType.INT -> Type.LONG_TYPE
            MonicelliType.CHAR -> Type.CHAR_TYPE
            MonicelliType.FLOAT -> Type.FLOAT_TYPE
            MonicelliType.BOOL -> Type.BOOLEAN_TYPE
            MonicelliType.DOUBLE -> Type.DOUBLE_TYPE
            MonicelliType.VOID -> Type.VOID_TYPE
        }

        private fun GeneratorAdapter.pushDefaultValue(type: MonicelliType) {
            when (type) {
                MonicelliType.INT -> push(0L)
                MonicelliType.CHAR -> push(0)
                MonicelliType.FLOAT -> push(0.0f)
                MonicelliType.BOOL -> push(false)
                MonicelliType.DOUBLE -> push(0.0)
                MonicelliType.VOID -> push(0)
            }
        }
    }
}