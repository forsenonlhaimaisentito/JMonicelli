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

package org.catafratta.jmonicelli

import org.catafratta.jmonicelli.cli.CommandLineInterface
import org.catafratta.jmonicelli.cli.impl.ClicktCommandLineInterface
import org.catafratta.jmonicelli.compiler.ModuleCompiler
import org.catafratta.jmonicelli.compiler.impl.AsmModuleCompiler
import org.catafratta.jmonicelli.parser.ModuleParser
import org.catafratta.jmonicelli.parser.impl.AntlrModuleParser
import org.koin.dsl.module

object MainModule {
    val koin = module {
        factory<ModuleParser> { AntlrModuleParser() }
        factory<ModuleCompiler> { AsmModuleCompiler() }
        factory<CommandLineInterface> { ClicktCommandLineInterface() }
    }
}