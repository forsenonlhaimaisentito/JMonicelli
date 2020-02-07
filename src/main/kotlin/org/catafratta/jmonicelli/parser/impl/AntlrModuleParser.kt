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

import org.antlr.v4.runtime.BailErrorStrategy
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.catafratta.jmonicelli.ast.MonicelliModule
import org.catafratta.jmonicelli.parser.ModuleParser
import org.catafratta.jmonicelli.parser.ParseException
import org.catafratta.jmonicelli.parser.antlr.MonicelliLexer
import org.catafratta.jmonicelli.parser.antlr.MonicelliParser
import java.io.Reader


class AntlrModuleParser : ModuleParser {
    override fun parse(source: Reader, sourceName: String): MonicelliModule {
        source
            .let { CharStreams.fromReader(it) }
            .let { MonicelliLexer(it) }
            .let { CommonTokenStream(it) }
            .let { MonicelliParser(it) }
            .apply {
                errorHandler = BailErrorStrategy()  // TODO: Extend to produce more friendly error messages

                val ast = try {
                    module()
                } catch (e: ParseCancellationException) {
                    throw ParseException(e)
                }

                return ast.accept(AntlrModuleVisitor(sourceName))
            }
    }
}