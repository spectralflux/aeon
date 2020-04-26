package com.spectralflux.aeon.interpreter;

import com.spectralflux.aeon.callable.AeonCallable;
import com.spectralflux.aeon.error.ErrorHandler;
import com.spectralflux.aeon.error.RuntimeError;
import com.spectralflux.aeon.lib.Print;
import com.spectralflux.aeon.syntax.expression.Assign;
import com.spectralflux.aeon.syntax.expression.Call;
import com.spectralflux.aeon.syntax.expression.Expr;
import com.spectralflux.aeon.syntax.expression.ExprVisitor;
import com.spectralflux.aeon.syntax.expression.Get;
import com.spectralflux.aeon.syntax.expression.Literal;
import com.spectralflux.aeon.syntax.expression.Set;
import com.spectralflux.aeon.syntax.expression.Variable;
import com.spectralflux.aeon.syntax.statement.Expression;
import com.spectralflux.aeon.syntax.statement.Function;
import com.spectralflux.aeon.syntax.statement.Let;
import com.spectralflux.aeon.syntax.statement.Stmt;

import com.spectralflux.aeon.syntax.statement.StmtVisitor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Interpreter implements ExprVisitor<Object>, StmtVisitor<Void> {

  private final ErrorHandler errorHandler;

  private final Environment globals = new Environment();
  private Environment environment = globals;
  private final Map<Expr, Integer> locals = new HashMap<>();

  public Interpreter(ErrorHandler errorHandler) {
    this.errorHandler = errorHandler;
    defineNativeFunctions();
  }

  private void defineNativeFunctions() {
    globals.define("print", new Print());
  }

  public void interpret(List<Stmt> statements) {
    try {
      for (Stmt statement : statements) {
        execute(statement);
      }
    } catch (RuntimeError error) {
      errorHandler.runtimeError(error);
    }
  }

  public void executeBlock(List<Stmt> statements, Environment environment) {
    Environment previous = this.environment;
    try {
      this.environment = environment;

      for (Stmt statement : statements) {
        execute(statement);
      }
    } finally {
      this.environment = previous;
    }
  }

  private void execute(Stmt stmt) {
    stmt.accept(this);
  }

  public Object evaluate(Expr expr) {
    return expr.accept(this);
  }

  @Override
  public Object visitLiteralExpr(Literal expr) {
    return expr.getValue();
  }

  @Override
  public Object visitCallExpr(Call expr) {
    Object callee = evaluate(expr.getCallee());

    List<Object> arguments = new ArrayList<>();
    for (Expr argument : expr.getArguments()) {
      arguments.add(evaluate(argument));
    }

    if (!(callee instanceof AeonCallable)) {
      throw new RuntimeError(expr.getParen(),
          "Can only call functions and classes.");
    }

    AeonCallable function = (AeonCallable) callee;
    if (arguments.size() != function.arity()) {
      throw new RuntimeError(expr.getParen(), "Expected " +
          function.arity() + " arguments but got " +
          arguments.size() + ".");
    }

    return function.call(this, arguments);
  }

  @Override
  public Void visitExpressionStmt(Expression stmt) {
    evaluate(stmt.getExpression());
    return null;
  }

  @Override
  public Object visitVariableExpr(Variable expr) {
    return lookUpVariable(expr.getName(), expr);
  }

  private Object lookUpVariable(Token name, Expr expr) {
    Integer distance = locals.get(expr);
    if (distance != null) {
      return environment.getAt(distance, name.getLexeme());
    } else {
      return globals.get(name);
    }
  }

  // TODO implement visitor methods

  @Override
  public Object visitGetExpr(Get expr) {
    return null;
  }

  @Override
  public Object visitSetExpr(Set expr) {
    return null;
  }

  @Override
  public Object visitAssignExpr(Assign expr) {
    return null;
  }

  @Override
  public Void visitFunctionStmt(Function stmt) {
    return null;
  }

  @Override
  public Void visitLetStmt(Let stmt) {
    return null;
  }
}
