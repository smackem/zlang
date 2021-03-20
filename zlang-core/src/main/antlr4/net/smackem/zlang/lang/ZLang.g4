grammar ZLang;

program
    : globalDecl* EOF
    ;

globalDecl
    : functionDecl
    | bindingStmt
    | typeDecl
    ;

functionDecl
    : DocComment? Fn (Ident ColonColon)? Ident LParen parameters? RParen returnType? block LineBreak
    ;

parameters
    : parameter (Comma parameter)*
    ;

parameter
    : Ident Colon type
    ;

returnType
    : Arrow type
    ;

type
    : Mutable? simpleType (LBracket RBracket)?
    ;

simpleType
    : Int
    | Float
    | Bool
    | Byte
    | String
    | Any
    | Ident
    ;

typeDecl
    : structDecl
    | unionDecl
    ;

structDecl
    : Struct Ident LBrace (parameter LineBreak)* RBrace LineBreak
    ;

unionDecl
    : Union Ident LBrace (parameter LineBreak)* RBrace LineBreak
    ;

statement
    : (bindingStmt
    | varDeclStmt
    | assignStmt
    | invocationStmt
    | ifStmt
    | forStmt
    | whileStmt
    | logStmt
    | panicStmt
    | assertStmt
    | swapStmt
    | returnStmt
    )? LineBreak
    ;

bindingStmt
    : DocComment? Let parameter Beq expr
    ;

varDeclStmt
    : DocComment? Var parameter Beq expr
    ;

assignStmt
    : Ident Beq expr
    | atom atomSuffix+ Beq expr
    ;

invocationStmt
    : Ident invocationSuffix
    | atom atomSuffix+
    ;

ifStmt
    : If expr block elseIfClause* elseClause?
    ;

elseIfClause
    : Else If expr block
    ;

elseClause
    : Else block
    ;

block
    : LBrace statement* RBrace
    ;

arguments
    : expr (Comma expr)*
    ;

forStmt
    : For parameter In expr whereClause? block
    ;

whereClause
    : Where expr
    ;

whileStmt
    : While expr block
    ;

logStmt
    : Log LParen arguments RParen
    ;

panicStmt
    : Panic LParen arguments RParen
    ;

assertStmt
    : Assert LParen expr RParen
    ;

swapStmt
    : Ident Swap Ident
    ;

returnStmt
    : Return expr
    ;

expr
    : condition (If condition Else expr)?
    ;

condition
    : comparison (conditionOp comparison)?
    ;

conditionOp
    : Or
    | And
    ;

comparison
    : tuple (comparator tuple)?
    ;

comparator
    : Eq
    | Lt
    | Le
    | Gt
    | Ge
    | Ne
    | In
    ;

tuple
    : range
    | term
    ;

range
    : term FromTo term (FromTo term)?
    ;

term
    : product (termOp product)*
    ;

termOp
    : Plus
    | Minus
    ;

product
    : molecule (productOp molecule)*
    ;

productOp
    : Times
    | Div
    | Mod
    ;

molecule
    : atomPrefix? atom atomSuffix*
    ;

atomPrefix
    : Minus
    | Not
    ;

atomSuffix
    : memberSuffix
    | indexSuffix
    ;

memberSuffix
    : (Dot Ident) invocationSuffix?
    ;

indexSuffix
    : LBracket expr RBracket
    ;

invocationSuffix
    : LParen arguments? RParen
    ;

atom
    : literal
    | Self
    | Ident
    | list
    | functionInvocation
    | LParen expr RParen
    ;

literal
    : number
    | StringLiteral
    | True
    | False
    | Nil
    ;

functionInvocation
    : Ident invocationSuffix
    ;

list
    : LBracket arguments? Comma? RBracket
    ;

Plus        : '+';
Minus       : '-';
Times       : '*';
Div         : '/';
Mod         : '%';
Lt          : '<';
Le          : '<=';
Gt          : '>';
Ge          : '>=';
Eq          : '==';
Ne          : '!=';
FromTo      : '..';
Swap        : '<=>';
Pipe        : '|';
Colon       : ':';
ColonColon  : '::';

Arrow       : '->' LineBreak?;
Or          : 'or' LineBreak?;
And         : 'and' LineBreak?;
Beq         : '=' LineBreak?;
Dot         : '.' LineBreak?;
Comma       : ',' LineBreak?;
LBrace      : '{' LineBreak?;
RBrace      : '}';
LBracket    : '[' LineBreak?;
RBracket    : ']';
LParen      : '(' LineBreak?;
RParen      : ')';

Fn          : 'fn';
If          : 'if';
Else        : 'else';
Switch      : 'switch';
For         : 'for';
In          : 'in';
Not         : 'not';
True        : 'true';
False       : 'false';
Nil         : 'nil';
While       : 'while';
Return      : 'return';
Where       : 'where';
Panic       : 'panic';
Assert      : 'assert';
Let         : 'let';
Var         : 'var';
Log         : 'log';

Int         : 'int';
Float       : 'float';
Bool        : 'bool';
String      : 'string';
Byte        : 'byte';
Any         : 'any';
Struct      : 'struct';
With        : 'with';
Self        : 'self';
New         : 'new';
Union       : 'union';
Mutable     : 'mutable';

number
    : (Plus | Minus)? Number
    ;

EnvironmentArg
    : '$' Ident
    ;

Ident
    : ('a' .. 'z' | 'A' .. 'Z' | '_') ('a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9')*
    ;

Number
    : [0-9][0-9_]* ('.' [0-9_]*[0-9])?
    ;

Color
    : '#' HexLiteral+ ('@' HexLiteral HexLiteral)?
    ;

HexLiteral
    : ('a' .. 'f' | 'A' .. 'F' | '0' .. '9')
    ;

StringLiteral
    : '"' ~["\\\r\n]* '"'
    ;

DocCommentLine
    : '///' ~ [\r\n]*
    ;

DocComment
    : (DocCommentLine LineBreak*)+
    ;

Comment
    : '//' ~ [\r\n]* -> skip
    ;

LineBreak
    : [\r\n]+
    ;

Ws
    : [ \t] -> skip
    ;