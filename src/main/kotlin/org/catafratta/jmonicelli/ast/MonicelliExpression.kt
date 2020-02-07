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

sealed class MonicelliExpression : AstNode {
    data class Call(
        val target: String,
        val args: List<MonicelliExpression>,
        override val source: AstNode.Location
    ) : MonicelliExpression()

    data class Mult(
        val lhs: MonicelliExpression,
        val rhs: MonicelliExpression,
        override val source: AstNode.Location
    ) : MonicelliExpression()

    data class Div(
        val lhs: MonicelliExpression,
        val rhs: MonicelliExpression,
        override val source: AstNode.Location
    ) : MonicelliExpression()

    data class Plus(
        val lhs: MonicelliExpression,
        val rhs: MonicelliExpression,
        override val source: AstNode.Location
    ) : MonicelliExpression()

    data class Minus(
        val lhs: MonicelliExpression,
        val rhs: MonicelliExpression,
        override val source: AstNode.Location
    ) : MonicelliExpression()

    data class Shift(
        val lhs: MonicelliExpression,
        val rhs: MonicelliExpression,
        val direction: Direction,
        override val source: AstNode.Location
    ) : MonicelliExpression() {
        enum class Direction { LEFT, RIGHT; }
    }

    data class Comparison(
        val lhs: MonicelliExpression,
        val rhs: MonicelliExpression,
        val operator: Operator,
        override val source: AstNode.Location
    ) : MonicelliExpression() {
        enum class Operator { LT, GT, LE, GE, EQ; }
    }

    data class Identifier(
        val name: String,
        override val source: AstNode.Location
    ) : MonicelliExpression()

    data class IntImmediate(val value: Long, override val source: AstNode.Location) : MonicelliExpression()

    data class FloatImmediate(val value: Double, override val source: AstNode.Location) : MonicelliExpression()
}