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

package org.catafratta.jmonicelli.parser.impl

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.catafratta.jmonicelli.ast.*
import org.catafratta.jmonicelli.parser.antlr.MonicelliBaseVisitor
import org.catafratta.jmonicelli.parser.antlr.MonicelliParser

/**
 * Converts an ANTLR4 Monicelli AST into a JMonicelli AST.
 */
class AntlrModuleVisitor(private val sourceName: String = "<unknown>") : MonicelliBaseVisitor<MonicelliModule>() {
    override fun visitModule(ctx: MonicelliParser.ModuleContext): MonicelliModule {
        return MonicelliModule(sourceName, emptyList(), null) + super.visitModule(ctx)
    }

    override fun visitFunction(ctx: MonicelliParser.FunctionContext): MonicelliModule {
        return MonicelliModule("", listOf(ctx.toMonicelliFunction()), null)
    }

    override fun visitMain(ctx: MonicelliParser.MainContext): MonicelliModule {
        val body = ctx.statement().map { it.toMonicelliStatementAll() }
        val main = MonicelliFunction(
            "<main>",
            MonicelliType.INT,
            emptyList(),
            body,
            ctx.getSourceLocation()
        )

        return MonicelliModule("", emptyList(), main)
    }

    override fun defaultResult(): MonicelliModule? {
        return MonicelliModule("<unknown>", emptyList(), null)
    }

    override fun aggregateResult(aggregate: MonicelliModule, nextResult: MonicelliModule): MonicelliModule {
        return aggregate + nextResult
    }


    private fun MonicelliParser.FunctionContext.toMonicelliFunction(): MonicelliFunction {
        val name = functionDecl().IDENTIFIER().symbol.text
        val returnType = functionDecl().typename().type.toMonicelliType()
        val params = functionDecl().getParams()
        val body = statement().map { it.toMonicelliStatementAll() }

        return MonicelliFunction(name, returnType, params, body, getSourceLocation())
    }


    private fun MonicelliParser.StatementContext.toMonicelliStatementAll(): MonicelliStatement {
        if (children.isNullOrEmpty()) throw ParseCancellationException()

        return when (val child = this.children[0]) {
            is MonicelliParser.ReadContext -> child.toMonicelliStatement()
            is MonicelliParser.PrintContext -> child.toMonicelliStatement()
            is MonicelliParser.VariableContext -> child.toMonicelliStatement()
            is MonicelliParser.AssignmentContext -> child.toMonicelliStatement()
            is MonicelliParser.ReturnStmtContext -> child.toMonicelliStatement()
            is MonicelliParser.CallContext -> child.toMonicelliStatement()
            is MonicelliParser.AbortContext -> child.toMonicelliStatement()
            is MonicelliParser.AssertStmtContext -> child.toMonicelliStatement()
            is MonicelliParser.LoopContext -> child.toMonicelliStatement()
            is MonicelliParser.BranchContext -> child.toMonicelliStatement()
            else -> throw IllegalArgumentException(javaClass.name + toStringTree())
        }
    }

    private fun MonicelliParser.ReadContext.toMonicelliStatement(): MonicelliStatement.Read {
        return MonicelliStatement.Read(identifier().IDENTIFIER().symbol.text, getSourceLocation())
    }

    private fun MonicelliParser.PrintContext.toMonicelliStatement(): MonicelliStatement.Print {
        return MonicelliStatement.Print(expr().toMonicelliExpressionAll(), getSourceLocation())
    }

    private fun MonicelliParser.VariableContext.toMonicelliStatement(): MonicelliStatement.Variable {
        val name = identifier().IDENTIFIER().text
        val type = typename().type.toMonicelliType()
        val initialValue = expr()?.toMonicelliExpressionAll()
        return MonicelliStatement.Variable(name, type, initialValue, getSourceLocation())
    }

    private fun MonicelliParser.AssignmentContext.toMonicelliStatement(): MonicelliStatement.Assignment {
        val target = identifier().IDENTIFIER().symbol.text
        val value = expr().toMonicelliExpressionAll()
        return MonicelliStatement.Assignment(target, value, getSourceLocation())
    }

    private fun MonicelliParser.ReturnStmtContext.toMonicelliStatement(): MonicelliStatement.Return {
        return MonicelliStatement.Return(expr().toMonicelliExpressionAll(), getSourceLocation())
    }

    private fun MonicelliParser.CallContext.toMonicelliStatement(): MonicelliStatement.Call {
        val target = IDENTIFIER().symbol.text
        val args = callArgs()?.args?.map { it.toMonicelliExpressionAll() } ?: emptyList()

        return MonicelliStatement.Call(target, args, getSourceLocation())
    }

    private fun MonicelliParser.AbortContext.toMonicelliStatement(): MonicelliStatement.Abort {
        return MonicelliStatement.Abort(getSourceLocation())
    }

    private fun MonicelliParser.AssertStmtContext.toMonicelliStatement(): MonicelliStatement.Assert {
        return MonicelliStatement.Assert(expr().toMonicelliExpressionAll(), getSourceLocation())
    }

    private fun MonicelliParser.LoopContext.toMonicelliStatement(): MonicelliStatement.Loop {
        val cond = expr().toMonicelliExpressionAll()
        val body = statement().map { it.toMonicelliStatementAll() }
        return MonicelliStatement.Loop(cond, body, getSourceLocation())
    }

    private fun MonicelliParser.BranchContext.toMonicelliStatement(): MonicelliStatement.Branch {
        val target = identifier().IDENTIFIER().symbol.text
        val targetNode = MonicelliExpression.Identifier(target, getSourceLocation())

        // TODO: Add semi-expressions as first class AST nodes
        // As a shortcut, we generate synthetic comparison expressions for now
        val paths = branchAlternative().map { alt ->
            val expr =
                alt.semiExpr()?.toMonicelliExpression(targetNode)
                    ?: MonicelliExpression.Comparison(
                        targetNode,
                        alt.expr().toMonicelliExpressionAll(),
                        MonicelliExpression.Comparison.Operator.EQ,
                        getSourceLocation()
                    )
            expr to alt.statement().map { it.toMonicelliStatementAll() }
        }
        val defaultPath = branchElse()?.statement()?.map { it.toMonicelliStatementAll() }

        return MonicelliStatement.Branch(target, paths, defaultPath, getSourceLocation())
    }


    private fun MonicelliParser.SemiExprContext.toMonicelliExpression(lhs: MonicelliExpression): MonicelliExpression.Comparison {
        val operator = op.toComparisonOperator()
        val rhs = expr().toMonicelliExpressionAll()
        return MonicelliExpression.Comparison(lhs, rhs, operator, getSourceLocation())
    }

    private fun MonicelliParser.ExprContext.toMonicelliExpressionAll(): MonicelliExpression {
        return when (this) {
            is MonicelliParser.CallExprContext -> call().toMonicelliExpression()
            is MonicelliParser.MultDivExprContext -> toMonicelliExpression()
            is MonicelliParser.PlusMinusExprContext -> toMonicelliExpression()
            is MonicelliParser.ShiftExprContext -> toMonicelliExpression()
            is MonicelliParser.ComparisonExprContext -> toMonicelliExpression()
            is MonicelliParser.IdentifierExprContext -> toMonicelliExpression()
            is MonicelliParser.ImmediateExprContext -> toMonicelliExpression()
            else -> throw IllegalArgumentException(javaClass.name)
        }
    }

    private fun MonicelliParser.CallContext.toMonicelliExpression(): MonicelliExpression.Call {
        val target = IDENTIFIER().symbol.text
        val args = callArgs()?.args?.map { it.toMonicelliExpressionAll() } ?: emptyList()

        return MonicelliExpression.Call(target, args, getSourceLocation())
    }

    // TODO: Generalize binary operators
    private fun MonicelliParser.MultDivExprContext.toMonicelliExpression(): MonicelliExpression {
        val left = lhs.toMonicelliExpressionAll()
        val right = rhs.toMonicelliExpressionAll()

        return if (op.type == MonicelliParser.TIMES) {
            MonicelliExpression.Mult(left, right, getSourceLocation())
        } else {
            MonicelliExpression.Div(left, right, getSourceLocation())
        }
    }

    private fun MonicelliParser.PlusMinusExprContext.toMonicelliExpression(): MonicelliExpression {
        val left = lhs.toMonicelliExpressionAll()
        val right = rhs.toMonicelliExpressionAll()

        return if (op.type == MonicelliParser.PLUS) {
            MonicelliExpression.Plus(left, right, getSourceLocation())
        } else {
            MonicelliExpression.Minus(left, right, getSourceLocation())
        }
    }

    private fun MonicelliParser.ShiftExprContext.toMonicelliExpression(): MonicelliExpression.Shift {
        val left = lhs.toMonicelliExpressionAll()
        val right = rhs.toMonicelliExpressionAll()
        val oper = op.toShiftOperator()
        return MonicelliExpression.Shift(left, right, oper, getSourceLocation())
    }

    private fun MonicelliParser.ComparisonExprContext.toMonicelliExpression(): MonicelliExpression.Comparison {
        val left = lhs.toMonicelliExpressionAll()
        val right = rhs.toMonicelliExpressionAll()
        val oper = op.toComparisonOperator()
        return MonicelliExpression.Comparison(left, right, oper, getSourceLocation())
    }

    private fun MonicelliParser.IdentifierExprContext.toMonicelliExpression(): MonicelliExpression.Identifier {
        return MonicelliExpression.Identifier(identifier().IDENTIFIER().symbol.text, getSourceLocation())
    }

    private fun MonicelliParser.ImmediateExprContext.toMonicelliExpression(): MonicelliExpression {
        immediate().INTEGER()?.let {
            return MonicelliExpression.IntImmediate(it.symbol.text.toLong(), getSourceLocation())
        }

        immediate().DOUBLE()?.let {
            return MonicelliExpression.FloatImmediate(it.symbol.text.toDouble(), getSourceLocation())
        }

        throw IllegalArgumentException(toStringTree())
    }

    private fun Token?.toMonicelliType(): MonicelliType = when (this?.type) {
        MonicelliParser.NECCHI -> MonicelliType.INT
        MonicelliParser.MASCETTI -> MonicelliType.CHAR
        MonicelliParser.PEROZZI -> MonicelliType.FLOAT
        MonicelliParser.MELANDRI -> MonicelliType.BOOL
        MonicelliParser.SASSAROLI -> MonicelliType.DOUBLE
        else -> MonicelliType.VOID
    }

    private fun MonicelliParser.FunctionDeclContext.getParams(): List<MonicelliFunction.Param> {
        return functionParams()?.functionParam()?.map {
            MonicelliFunction.Param(it.IDENTIFIER().symbol.text, it.typename().type.toMonicelliType())
        } ?: emptyList()
    }

    private fun Token.toComparisonOperator(): MonicelliExpression.Comparison.Operator {
        return when (type) {
            MonicelliParser.LT -> MonicelliExpression.Comparison.Operator.LT
            MonicelliParser.GT -> MonicelliExpression.Comparison.Operator.GT
            MonicelliParser.LE -> MonicelliExpression.Comparison.Operator.LE
            MonicelliParser.GE -> MonicelliExpression.Comparison.Operator.GE
            else -> throw IllegalArgumentException(toString())
        }
    }

    private fun Token.toShiftOperator(): MonicelliExpression.Shift.Direction {
        return when (type) {
            MonicelliParser.LEFT -> MonicelliExpression.Shift.Direction.LEFT
            MonicelliParser.RIGHT -> MonicelliExpression.Shift.Direction.RIGHT
            else -> throw IllegalArgumentException(toString())
        }
    }

    private fun ParserRuleContext.getSourceLocation(): AstNode.Location = start.getSourceLocation()

    private fun Token.getSourceLocation(): AstNode.Location {
        return AstNode.Location(sourceName, line, charPositionInLine)
    }

    companion object {
        private operator fun MonicelliModule.plus(other: MonicelliModule): MonicelliModule {
            return MonicelliModule(
                name = other.name.ifBlank { this.name },
                functions = functions + other.functions,
                mainFunction = mainFunction ?: other.mainFunction
            )
        }
    }
}