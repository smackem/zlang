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
    | (varDeclStmt LineBreak)
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
    : Mutable? simpleType typePostfix*
    ;

typePostfix
    : LBracket RBracket
    | List
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
    : DocComment? Let parameter Beq expr
    ;

varDeclStmt
    : DocComment? Var parameter (Beq expr)?
    ;

assignStmt
    : Ident Beq expr
    | postFixedPrimary Beq expr
    ;

invocationStmt
    : functionInvocation
    | methodInvocation
    ;

methodInvocation
    : primary methodInvocationPostfix
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
    : Log expr
    ;

panicStmt
    : Panic expr
    ;

assertStmt
    : Assert expr
    ;

swapStmt
    : Ident Swap Ident
    ;

returnStmt
    : Return expr?
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
    : typeCheckExpr
    | typeCheckExpr relationalOp typeCheckExpr?
    ;

relationalOp
    : Eq
    | Lt
    | Le
    | Gt
    | Ge
    | Ne
    | In
    ;

typeCheckExpr
    : additiveExpr
    | additiveExpr Is type
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
    | functionInvocation
    | instanceCreation
    | switchOverUnion
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
    : structInstanceCreation
    | unionInstanceCreation
    | arrayInstanceCreation
    | listInstanceCreation
    | listInstanceCreationFromArray
    ;

structInstanceCreation
    : New Ident LBrace (fieldInitializer LineBreak)* RBrace
    ;

unionInstanceCreation
    : New Ident Dot Ident LParen expr RParen
    ;

fieldInitializer
    : Ident Colon expr
    ;

arrayInstanceCreation
    : New type LBracket expr RBracket
    | New type LBracket RBracket LBrace arguments? LineBreak? RBrace
    ;

listInstanceCreation
    : New type List LBrace arguments? LineBreak? RBrace
    ;

listInstanceCreationFromArray
    : New type List LParen expr RParen
    ;

switchOverUnion
    : Switch expr LBrace switchUnionFieldClause+ switchUnionElseClause? RBrace
    ;

switchUnionFieldClause
    : parameter Arrow expr LineBreak
    ;

switchUnionElseClause
    : Else Arrow expr LineBreak
    ;

literal
    : number
    | StringLiteral
    | CharLiteral
    | True
    | False
    | Nil
    | NullPtr
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
List        : 'list';

number
    : (Plus | Minus)? (IntegerNumber | RealNumber)
    ;

Ident
    : ('a' .. 'z' | 'A' .. 'Z' | '_') ('a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9')*
    ;

IntegerNumber
    : [0-9][0-9_]*
    ;

RealNumber
    : [0-9][0-9_]* '.' [0-9_]*[0-9]
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

CharLiteral
    : '\'' ~['\\\r\n] '\''
    ;

DocCommentLine
    : '///' ~[\r\n]*
    ;

DocComment
    : (DocCommentLine LineBreak*)+
    ;

Comment
    : '//' ~[\r\n]* -> skip
    ;

LineBreak
    : [\r\n]+
    ;

Ws
    : [ \t] -> skip
    ;