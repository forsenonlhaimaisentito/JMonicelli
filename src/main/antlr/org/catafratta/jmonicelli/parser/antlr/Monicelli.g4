grammar Monicelli;

@header {
package org.catafratta.jmonicelli.parser.antlr;
}

module : function* main? function* EOF ;

main : MAIN statement+ ;

function : functionDecl statement* ;
functionDecl : FUN_DECL typename? IDENTIFIER (FUN_PARAMS functionParams)? FUN_END ;
functionParams : params+=functionParam (COMMA params+=functionParam)* ;
functionParam : IDENTIFIER typename ;

statement
    : read
    | print
    | variable
    | assignment
    | returnStmt
    | call
    | abort
    | assertStmt
    | loop
    | branch
    ;

read : INPUT identifier ;
print : expr PRINT ;

variable : VAR_DECL identifier COMMA typename (ASSIGN expr)? ;
assignment : identifier ASSIGN expr ;

returnStmt : RETURN expr? BANG ;

branch : BRANCH identifier QMARK branchAlternative+ branchElse? BRANCH_END;
branchAlternative
    : (expr | semiExpr) COLON statement+
    | BRANCH_CASE (expr | semiExpr) COLON statement+
    ;
branchElse : BRANCH_ELSE COLON statement+ ;

loop: LOOP statement+ LOOP_COND expr ;

abort : ABORT ;

assertStmt : ASSERT expr BANG ;

semiExpr : op=(GT | LT | GE | LE) expr ;

// Using C-style operator precedence
// See https://en.cppreference.com/w/c/language/operator_precedence
expr
    : call # CallExpr
    | lhs=expr op=(TIMES | OVER) rhs=expr # MultDivExpr
    | lhs=expr op=(PLUS | MINUS) rhs=expr # PlusMinusExpr
    | lhs=expr SHIFT op=(LEFT | RIGHT) TIMES rhs=expr # ShiftExpr
    | lhs=expr op=(GT | LT | GE | LE) rhs=expr # ComparisonExpr
    | identifier # IdentifierExpr
    | immediate # ImmediateExpr
    ;

call : FUN_CALL IDENTIFIER (FUN_PARAMS callArgs)? FUN_END ;
callArgs : args+=expr (COMMA args+=expr)* ;

immediate : INTEGER | DOUBLE ;
identifier : IDENTIFIER | ARTICLE IDENTIFIER ;
typename : type=(NECCHI | MASCETTI | PEROZZI | MELANDRI | SASSAROLI) ;

ARTICLE : 'il' | 'lo' | 'la' | 'l\'' | 'i' | 'gli' | 'le' | 'un' | 'un\'' | 'una' | 'dei' | 'delle' ;
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

DI : 'di' | 'del' | 'della' | 'dell\'' ;  // What about "degli"/"delle"?
PREMATURA : [bp] 'rematura' ;
SUPERCAZZOLA : 'supercazzo' [lr] 'a' ;

fragment ACCENT_VOWEL : A_GRAVE | E_GRAVE | '\u00E9' | '\u00EC' | '\u00F2' | '\u00F3' | U_GRAVE | [aeiou] '`';
fragment A_GRAVE : '\u00E0' | 'a`' ;
fragment E_GRAVE : '\u00E8' | 'e`' ;
fragment U_GRAVE : '\u00F9' | 'u`' ;

BANG : '!' ;
QMARK : '?' ;
COLON : ':' ;
COMMA : ',' ;

COMMENT_START : '#' | 'bituma' ;
LINE_COMMENT: COMMENT_START ~[\r\n]* -> skip ;

INTEGER : [+\-]? DIGIT+ ;
DOUBLE : [+\-]? (DIGIT* '.' DIGIT+ | DIGIT+ '.'?) [eE] [+\-]? DIGIT+
        | [+\-]? (DIGIT* '.' DIGIT+ | DIGIT+ '.');
fragment DIGIT : [0-9] ;

PLUS : 'pi' U_GRAVE ;
MINUS : 'meno' ;
TIMES : 'per' ;
OVER : 'diviso' ;
SHIFT : 'con scappellamento a' ;
LEFT : 'sinistra' ;
RIGHT : 'destra' ;
LT : 'minore ' DI ;
GT : 'maggiore ' DI ;
LE : 'minore o uguale ' ('a' | DI);
GE : 'maggiore o uguale ' ('a' | DI);

RETURN : 'vaffanzum' ;
VAR_DECL : 'voglio';
ASSIGN : 'come ' 'se '? 'fosse' ;
PRINT : 'a posterdati' ;
INPUT  : 'mi porga' ;
ASSERT : 'ho visto' ;
ABORT : 'avvertite don ulrico' ;

LOOP : 'stuzzica' ;
LOOP_COND : 'e ' PREMATURA ' anche, se' ;

BRANCH : 'che cos\'' E_GRAVE ;
BRANCH_CASE : 'o magari' ;
BRANCH_ELSE : 'o tarapia tapioco' ;
BRANCH_END : 'e velocit' A_GRAVE ' di esecuzione' ;

MAIN : 'Lei ha clacsonato' ;
FUN_DECL : 'blinda la ' SUPERCAZZOLA ;
FUN_PARAMS : 'con' ;
FUN_CALL : PREMATURA 'ta la ' SUPERCAZZOLA ;
FUN_END : 'o scherziamo' '?'? ;

// TYPENAME : NECCHI | MASCETTI | PEROZZI | MELANDRI | SASSAROLI ;

NECCHI : 'Necchi' ;
MASCETTI : 'Mascetti' ;
PEROZZI : 'Perozzi' ;
MELANDRI : 'Melandri' ;
SASSAROLI : 'Sassaroli' ;

IDENTIFIER : ([A-Za-z] | ACCENT_VOWEL) ([A-Za-z] | DIGIT | ACCENT_VOWEL)* ;

WS : [ \t\r\n]+ -> skip ;