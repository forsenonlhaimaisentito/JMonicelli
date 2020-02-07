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

package org.catafratta.jmonicelli.cli.impl

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.validate
import com.github.ajalt.clikt.parameters.types.file
import org.antlr.v4.runtime.RecognitionException
import org.catafratta.jmonicelli.compiler.ModuleCompiler
import org.catafratta.jmonicelli.compiler.compile
import org.catafratta.jmonicelli.parser.ModuleParser
import org.catafratta.jmonicelli.parser.ParseException
import org.catafratta.jmonicelli.parser.parse
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.io.File
import kotlin.system.exitProcess

class ClicktMainCommand : CliktCommand(
    name = "JMonicelli",
    help = """
        The Monicelli 2.0 for JVM compiler
    """.trimIndent()
), KoinComponent {
    private val sourceFile: File by argument(name = "source", help = "The Monicelli source file path")
        .file(exists = true, readable = true)

    private val className: String by argument(
        name = "className",
        help = "The name of the resulting Java class, will determine the output file name. Packages are not supported yet."
    ).validate {
        if (!CLASSNAME_REGEX.matches(it)) fail("Invalid class name: '$it'")
    }

    override fun run() {
        val parser by inject<ModuleParser>()
        val compiler by inject<ModuleCompiler>()

        val ast = try {
            parser.parse(sourceFile)
        } catch (e: ParseException) {
            System.err.println("Compilation failed: a parse error has occurred (${e.detailsString()})")
            exitProcess(1)
        }

        compiler.compile(ast, File("$className.class"), className)

        println("Programma come se fosse compilato.")
    }

    companion object {
        private val CLASSNAME_REGEX = Regex("\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*")

        private fun ParseException.detailsString(): String {
            return cause?.cause?.let { e ->
                (e as? RecognitionException)?.let {
                    "at ${it.offendingToken.line}:${it.offendingToken.charPositionInLine}"
                }
            } ?: "Unknown error"
        }
    }
}