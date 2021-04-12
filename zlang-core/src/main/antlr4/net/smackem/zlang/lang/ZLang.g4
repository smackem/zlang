grammar ZLang;

module
    : moduleDecl? globalDecl* EOF
    ;

moduleDecl
    : Module Ident usesClause? LineBreak
    ;

usesClause
    : Uses Ident (Comma Ident)*
    ;

globalDecl
    : functionDecl
    | (bindingStmt LineBreak)
    | typeDecl
    ;

functionDecl
    : DocComment? Fn declaringTypePrefix? Ident LParen parameters? RParen returnType? block LineBreak
    ;

declaringTypePrefix
    : Ident ColonColon
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
    | RuntimePtr
    | Object
    | Ident
    ;

typeDecl
    : DocComment? (structDecl
    | unionDecl
    | interfaceDecl)
    ;

structDecl
    : Struct Ident LBrace (parameter LineBreak)* RBrace implementsClause? LineBreak
    ;

unionDecl
    : Union Ident LBrace (parameter LineBreak)* RBrace implementsClause? LineBreak
    ;

implementsClause
    : Is Ident (Comma Ident)*
    ;

interfaceDecl
    : Interface Ident LBrace (interfaceMethodDecl LineBreak)* RBrace LineBreak
    ;

interfaceMethodDecl
    : DocComment? Fn declaringTypePrefix? Ident LParen parameters? RParen returnType?
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
    : DocComment? Let parameter (Beq expr)?
    ;

varDeclStmt
    : DocComment? Var parameter (Beq expr)?
    ;

assignStmt
    : Ident Beq expr
    | primary Beq expr
    ;

invocationStmt
    : functionInvocation
    | primary methodInvocationPostfix
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
    : forIteratorStmt
    | forRangeStmt
    ;

forIteratorStmt
    : For parameter In expr whereClause? block
    ;

forRangeStmt
    : For parameter In range block
    ;

range
    : expr FromTo expr (FromTo expr)?
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
    : conditionalOrExpr
    | conditionalOrExpr If conditionalOrExpr Else expr
    ;

conditionalOrExpr
    : conditionalAndExpr
    | conditionalAndExpr Or conditionalOrExpr
    ;

conditionalAndExpr
    : relationalExpr
    | relationalExpr And conditionalAndExpr
    ;

relationalExpr
    : additiveExpr
    | additiveExpr relationalOp additiveExpr?
    ;

relationalOp
    : Eq
    | Lt
    | Le
    | Gt
    | Ge
    | Ne
    | In
    | Is
    ;

additiveExpr
    : multiplicativeExpr
    | additiveExpr additiveOp multiplicativeExpr
    ;

additiveOp
    : Plus
    | Minus
    ;

multiplicativeExpr
    : unaryExpr
    | multiplicativeExpr multiplicativeOp unaryExpr
    ;

multiplicativeOp
    : Times
    | Div
    | Mod
    ;

unaryExpr
    : unaryOp unaryExpr
    | castExpr
    | postFixedPrimary
    ;

unaryOp
    : Minus
    | Plus
    | Not
    ;

castExpr
    : LParen type RParen unaryExpr
    ;

postFixedPrimary
    : primary
    | postFixedPrimary methodInvocationPostfix
    | postFixedPrimary fieldAccessPostfix
    | postFixedPrimary arrayAccessPostfix
    ;

methodInvocationPostfix
    : Dot Ident LParen arguments? RParen
    ;

fieldAccessPostfix
    : Dot Ident
    ;

arrayAccessPostfix
    : LBracket expr RBracket
    ;

primary
    : literal
    | Self
    | Ident
    | list
    | functionInvocation
    | instanceCreation
    | LParen expr RParen
    ;

functionInvocation
    : userFunctionInvocation
    | runtimeFunctionInvocation
    ;

userFunctionInvocation
    : Ident LParen arguments? RParen
    ;

runtimeFunctionInvocation
    : Runtime ColonColon Ident LParen arguments? RParen
    ;

instanceCreation
    : structOrUnionInstanceCreation
    | arrayInstanceCreation
    ;

structOrUnionInstanceCreation
    : New Ident LBrace (fieldInitializer LineBreak)* RBrace
    ;

fieldInitializer
    : Ident Colon expr
    ;

arrayInstanceCreation
    : New simpleType LBracket expr RBracket
    ;

literal
    : number
    | StringLiteral
    | True
    | False
    | Nil
    | NullPtr
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
Is          : 'is';

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

Module      : 'module';
Uses        : 'uses';
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
NullPtr     : 'null_ptr';

Int         : 'int';
Float       : 'float';
Bool        : 'bool';
String      : 'string';
Byte        : 'byte';
Object      : 'object';
RuntimePtr  : 'runtime_ptr';
Struct      : 'struct';
With        : 'with';
Self        : 'self';
New         : 'new';
Union       : 'union';
Mutable     : 'mutable';
Runtime     : 'runtime';
Interface   : 'interface';

number
    : (Plus | Minus)? (IntegerNumber | RealNumber)
    ;

Ident
    : ('a' .. 'z' | 'A' .. 'Z' | '_') ('a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9')*
    ;

IntegerNumber
    : [0-9][0-9_]+
    ;

RealNumber
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