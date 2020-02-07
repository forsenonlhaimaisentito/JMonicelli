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
import org.koin.core.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.get

class Main private constructor(private val args: Array<String>) : KoinComponent {
    fun main() {
        get<CommandLineInterface>().main(args)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            startKoin {
                modules(MainModule.koin)
            }

            Main(args).main()
        }
    }
}