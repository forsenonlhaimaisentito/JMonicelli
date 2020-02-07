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

package org.catafratta.jmonicelli.ast

sealed class MonicelliStatement : AstNode {
    data class Read(val target: String, override val source: AstNode.Location) : MonicelliStatement()
    data class Print(val expression: MonicelliExpression, override val source: AstNode.Location) : MonicelliStatement()
    data class Variable(
        val name: String,
        val type: MonicelliType,
        val value: MonicelliExpression? = null,
        override val source: AstNode.Location
    ) : MonicelliStatement()

    data class Assignment(
        val target: String,
        val value: MonicelliExpression,
        override val source: AstNode.Location
    ) : MonicelliStatement()

    data class Return(
        val value: MonicelliExpression? = null,
        override val source: AstNode.Location
    ) : MonicelliStatement()

    data class Call(
        val target: String,
        val args: List<MonicelliExpression>,
        override val source: AstNode.Location
    ) : MonicelliStatement()

    data class Assert(val expression: MonicelliExpression, override val source: AstNode.Location) : MonicelliStatement()
    data class Abort(override val source: AstNode.Location) : MonicelliStatement()
    data class Loop(
        val condition: MonicelliExpression,
        val body: List<MonicelliStatement>,
        override val source: AstNode.Location
    ) : MonicelliStatement()

    data class Branch(
        val target: String,
        val paths: List<Pair<MonicelliExpression.Comparison, List<MonicelliStatement>>>,
        val defaultPath: List<MonicelliStatement>? = null,
        override val source: AstNode.Location
    ) : MonicelliStatement()
}