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

package org.catafratta.jmonicelli.parser

import java.io.File
import java.nio.charset.Charset

fun ModuleParser.parse(source: File, charset: Charset = Charsets.UTF_8) = source.reader(charset).use { parse(it, source.name) }

fun ModuleParser.parse(source: String) = parse(source.reader())