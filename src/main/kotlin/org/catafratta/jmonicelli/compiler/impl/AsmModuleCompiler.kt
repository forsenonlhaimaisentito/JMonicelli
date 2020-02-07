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

import org.catafratta.jmonicelli.ast.MonicelliModule
import org.catafratta.jmonicelli.compiler.ModuleCompiler
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.util.CheckClassAdapter
import java.io.OutputStream

class AsmModuleCompiler : ModuleCompiler {
    override fun compile(module: MonicelliModule, dest: OutputStream, className: String) {
        require(CLASSNAME_REGEX.matches(className)) { "Invalid class name" }

        val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES)

        ModuleGenerator(module, className, writer).generateClass()

        val bytecode = writer.toByteArray()

        try {
            checkClass(bytecode)
        } catch (e: Throwable){
            System.err.println("Class verification failed!")
            System.err.println("""
This is probably a bug in the compiler, please report the issue to the author, together with the following stack trace
and the source file that caused the error.
""".trimIndent())
            e.printStackTrace()
            return
        }

        dest.write(bytecode)
    }

    companion object {
        private val CLASSNAME_REGEX = Regex("\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*")

        private fun checkClass(bytecode: ByteArray){
            ClassReader(bytecode)
                .accept(CheckClassAdapter(ClassWriter(0)), 0)
        }
    }
}