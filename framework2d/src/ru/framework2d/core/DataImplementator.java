package ru.framework2d.core;

import java.util.ArrayList;

import ru.framework2d.data.Bool;
import ru.framework2d.data.DataInterface;
import ru.framework2d.data.DoubleFractional;
import ru.framework2d.data.Fractional;
import ru.framework2d.data.Numeral;
import ru.framework2d.data.Text;


import android.util.Log;

public class DataImplementator {

	private static final String LOG = "Implementator";
	
	private static interface Weights {
		final static int BOOLEAN_OR_AND = 0;
		final static int BOOLEAN = 1;
		final static int ADDITION = 2;
		final static int MULTIPLICATION = 3;
		final static int PARANTHESES = 4;
	}
	
	private static interface Operations {
		final static int ADDITION = 0;
		final static int SUBTRACTION = 1;
		final static int MULTIPLICATION = 2;
		final static int DIVISION = 3;
		
		final static int MORE = 4;
		final static int MORE_OR_SAME = 5;
		final static int LESS = 6;
		final static int LESS_OR_SAME = 7;
		final static int SAME = 8;

		final static int AND = 9;
		final static int OR = 10;
		final static int XOR = 11;
	}
	
	public static Property toProperty(String propertyName, String value) {
		DataInterface data = toData(value);
		return new Property(propertyName, data);
	}
	
	private static ArrayList <Operation> lstOperations = new ArrayList <Operation>();
	private static ArrayList <DataInterface> lstOperands = new ArrayList <DataInterface>();
	private static String operand = "";
	
	private static DataInterface toData(String expression) {
		boolean isExpression = true;
		
		if (expression.length() > 1 && 	expression.charAt(0) == '%' && 
										expression.charAt(1) == 's') {
			return new Text(expression.substring(2));
		}
		
		expression.replaceAll(" ", "");
		operand = "";
		
		int paranthesesNum = 0;
		int ch = 0;
		char character;
		while (ch < expression.length() && isExpression) {
			character = expression.charAt(ch);
			
			if (Character.isLetter(character) || (character == '#')) {
				if (character == 't' && ch + 4 <= expression.length() && 
						expression.substring(ch, ch + 4).contentEquals("true")) {
					lstOperands.add(new Bool(true));
					ch += 3;
				} else if (character == 'f' && ch + 5 <= expression.length() && 
						expression.substring(ch, ch + 5).contentEquals("false")) {
					lstOperands.add(new Bool(false));
					ch += 4;
				} else {
					isExpression = false;
				}
			} else if (Character.isDigit(character) || character == '.') {
				operand = operand + character;
			}
			
			switch (character)  {
				case '(' :
					paranthesesNum++;
					break;
				case ')' :
					paranthesesNum--;
					break;
				case '+' :
					lstOperations.add(new Operation(
							paranthesesNum * Weights.PARANTHESES + Weights.ADDITION, 
							Operations.ADDITION));
					if (operand.length() > 0) lstOperands.add(operandToData());
					break;
				case '-' :
					if (ch > 0) {
						if (expression.charAt(ch - 1) == '(') {
							operand = "-";
						} else {
							lstOperations.add(new Operation(
									paranthesesNum * Weights.PARANTHESES + Weights.ADDITION,
									Operations.SUBTRACTION));
							if (operand.length() > 0) lstOperands.add(operandToData());
						}
					} else {
						operand = "-";
					}
					break;
				case '*' :
					lstOperations.add(new Operation(
							paranthesesNum * Weights.PARANTHESES + Weights.MULTIPLICATION,
							Operations.MULTIPLICATION));
					if (operand.length() > 0) lstOperands.add(operandToData());
					break;
				case '/' :
					lstOperations.add(new Operation(
							paranthesesNum * Weights.PARANTHESES + Weights.MULTIPLICATION,
							Operations.DIVISION));
					if (operand.length() > 0) lstOperands.add(operandToData());
					break;
				case '=' :
					lstOperations.add(new Operation(
							paranthesesNum * Weights.PARANTHESES + Weights.BOOLEAN, 
							Operations.SAME));
					if (operand.length() > 0) lstOperands.add(operandToData());
					if (expression.length() > ch + 1 && expression.charAt(ch + 1) == '=') ch++;
					break;
				case '>' :
					if (expression.length() > ch + 1 && expression.charAt(ch + 1) == '=') {
						lstOperations.add(new Operation(
								paranthesesNum * Weights.PARANTHESES + Weights.BOOLEAN, 
								Operations.MORE_OR_SAME));
						ch++;
					} else {
						lstOperations.add(new Operation(
								paranthesesNum * Weights.PARANTHESES + Weights.BOOLEAN, 
								Operations.MORE));
					}
					if (operand.length() > 0) lstOperands.add(operandToData());
					break;
				case '<' :
					if (expression.length() > ch + 1 && expression.charAt(ch + 1) == '=') {
						lstOperations.add(new Operation(
								paranthesesNum * Weights.PARANTHESES + Weights.BOOLEAN, 
								Operations.LESS_OR_SAME));
						ch++;
					} else {
						lstOperations.add(new Operation(
								paranthesesNum * Weights.PARANTHESES + Weights.BOOLEAN, 
								Operations.LESS));
					}
					if (operand.length() > 0) lstOperands.add(operandToData());
					break;
				case '&' :
					if (expression.length() > ch + 1 && expression.charAt(ch + 1) == '&') {
						lstOperations.add(new Operation(
								paranthesesNum * Weights.PARANTHESES + Weights.BOOLEAN_OR_AND, 
								Operations.AND));
						if (operand.length() > 0) lstOperands.add(operandToData());
						ch++;
					}
					break;
				case '|' :
					if (expression.length() > ch + 1 && expression.charAt(ch + 1) == '|') {
						lstOperations.add(new Operation(
								paranthesesNum * Weights.PARANTHESES + Weights.BOOLEAN_OR_AND,
								Operations.OR));
						if (operand.length() > 0) lstOperands.add(operandToData());
						ch++;
					}
					break;
				default:
					break;
			}
			
			ch++;
		}
		if (operand.length() > 0) lstOperands.add(operandToData());

		BinaryNode head;
		if (isExpression) {
			if (paranthesesNum == 0) {
				
				if (!lstOperations.isEmpty()) {
					head = readNode(0, lstOperations.size() - 1);
					head.calculate();
					lstOperations.clear();
					lstOperands.clear();
				} else {
					head = new BinaryNode();
					head.result = lstOperands.get(0);
					lstOperands.clear();
				}
				
			} else {
				
				Log.d(LOG, "This is not valid expression: <" + expression + ">");
				lstOperations.clear();
				lstOperands.clear();
				return new Text(expression);
				
			}
		} else {
			lstOperations.clear();
			lstOperands.clear();
			return new Text(expression);
		}
		return head.result;
		
	}
	private static DataInterface operandToData() {
		try {
			int integer = Integer.decode(operand);
			operand = "";
			return new Numeral(integer); 
		} catch (NumberFormatException ei) {
			Log.i(LOG, "it's not an integer: " + operand);
			try {
				float fractional = Float.parseFloat(operand);
				operand = "";
				return new Fractional(fractional); 
			} catch (NumberFormatException ef) {
				Log.i(LOG, "it's not a float: " + operand);
				try {
					double doubleFractional = Double.parseDouble(operand);
					operand = "";
					return new DoubleFractional(doubleFractional); 
				} catch (NumberFormatException ed) {
					Log.i(LOG, "it's not a double: " + operand);
				}
			}
		}
		DataInterface result = new Text(operand); 
		operand = "";
		return result;
	}
	
	private static BinaryNode readNode(int start, int end){
		BinaryNode node = new DataImplementator.BinaryNode();
		if (start == end) {
			node.result = calculateData(lstOperands.get(start), 
										lstOperands.get(start + 1), 
										lstOperations.get(start).operator
										);
			node.calculated = true;
		} else if (start < end) {
			int currentWeight = 0;
			int currentNo = start;
			
			if (lstOperations.size() > end) { 
				currentWeight = lstOperations.get(start).weight;
				for (int i = start; i <= end; i++) {
					if (lstOperations.get(i).weight < currentWeight) {
						currentWeight = lstOperations.get(i).weight;
						currentNo = i;
					}
				}
			}
			node.operator = lstOperations.get(currentNo).operator;
			node.left = readNode(start, currentNo - 1);
			node.right = readNode(currentNo + 1, end);
		} else {
			node.result = lstOperands.get(start);
			node.calculated = true;
		}
		return node;
	}
	
	private static class Operation {
		
		int weight; 
		int operator;
		
		Operation(int weight, int operator) {
			this.weight = weight;
			this.operator = operator;
		}
	}
	

	
	private static class BinaryNode {
		
		public BinaryNode left;
		public BinaryNode right;
		
		public boolean calculated = false;
		
		DataInterface result;
		
		public int operator;
		
		
		
		public void calculate() {
			if (!calculated) {
				if (!left.calculated) left.calculate();
				if (!right.calculated) right.calculate();

				this.result = calculateData(left.result, right.result, operator);
				
				if (this.result != null) calculated = true;
			}
		}
		BinaryNode(){}
	}
	private static DataInterface calculateData(	DataInterface firstOperand, 
												DataInterface secondOperand, int operator) {
		DataInterface result = null;
		if (firstOperand instanceof Numeral) {
			if (secondOperand instanceof Numeral) {
				result = getResult(
						((Numeral) firstOperand).value, 
						((Numeral) secondOperand).value,
						operator
						);
			} else if (secondOperand instanceof Fractional) {
				result = getResult(
						(float) ((Numeral) firstOperand).value, 
						((Fractional) secondOperand).value,
						operator
						);
			} else if (secondOperand instanceof DoubleFractional) {
				result = getResult(
						(double) ((Numeral) firstOperand).value, 
						((DoubleFractional) secondOperand).value,
						operator
						);
			}
		}
		else if (firstOperand instanceof Fractional) {
			if (secondOperand instanceof Numeral) {
				result = getResult(
						((Fractional) firstOperand).value, 
						(float)((Numeral) secondOperand).value,
						operator
						);
			} else if (secondOperand instanceof Fractional) {
				result = getResult(
						((Fractional) firstOperand).value, 
						((Fractional) secondOperand).value,
						operator
						);
			} else if (secondOperand instanceof DoubleFractional) {
				result = getResult(
						(double) ((Fractional) firstOperand).value, 
						((DoubleFractional) secondOperand).value,
						operator
						);
			}
		}
		else if (firstOperand instanceof DoubleFractional) {
			if (secondOperand instanceof Numeral) {
				result = getResult(
						((DoubleFractional) firstOperand).value, 
						(double) ((Numeral) secondOperand).value,
						operator
						);
			} else if (secondOperand instanceof Fractional) {
				result = getResult(
						((DoubleFractional) firstOperand).value, 
						(double) ((Fractional) secondOperand).value,
						operator
						);
			} else if (secondOperand instanceof DoubleFractional) {
				result = getResult(
						((DoubleFractional) firstOperand).value, 
						((DoubleFractional) secondOperand).value,
						operator
						);
			}
		} else if (firstOperand instanceof Bool) {
			if (secondOperand instanceof Bool) {
				result = getResult(
						((Bool) firstOperand).value, 
						((Bool) secondOperand).value,
						operator
						);
			}
		}
		return result;
	}
	private static DataInterface getResult(int firstOperand, int secondOperand, int operator) {
		switch (operator) {
		case Operations.ADDITION:
			return new Numeral(firstOperand + secondOperand);
		case Operations.SUBTRACTION:
			return new Numeral(firstOperand - secondOperand);
		case Operations.MULTIPLICATION:
			return new Numeral(firstOperand * secondOperand);
		case Operations.DIVISION:
			float result = (float) firstOperand / (float) secondOperand;
			int integer = (int) result; 
			if (integer == result) {
				return new Numeral(integer);
			}
			return new Fractional(result);
		case Operations.MORE:
			return new Bool(firstOperand > secondOperand);
		case Operations.MORE_OR_SAME:
			return new Bool(firstOperand >= secondOperand);
		case Operations.LESS:
			return new Bool(firstOperand < secondOperand);
		case Operations.LESS_OR_SAME:
			return new Bool(firstOperand <= secondOperand);
		case Operations.SAME:
			return new Bool(firstOperand == secondOperand);
		default:
			return new Text("" + firstOperand + secondOperand);
		}
	}
	private static DataInterface getResult(float firstOperand, float secondOperand, int operator) {
		switch (operator) {
		case Operations.ADDITION:
			return new Fractional(firstOperand + secondOperand);
		case Operations.SUBTRACTION:
			return new Fractional(firstOperand - secondOperand);
		case Operations.MULTIPLICATION:
			return new Fractional(firstOperand * secondOperand);
		case Operations.DIVISION:
			return new Fractional(firstOperand / secondOperand);
		case Operations.MORE:
			return new Bool(firstOperand > secondOperand);
		case Operations.MORE_OR_SAME:
			return new Bool(firstOperand >= secondOperand);
		case Operations.LESS:
			return new Bool(firstOperand < secondOperand);
		case Operations.LESS_OR_SAME:
			return new Bool(firstOperand <= secondOperand);
		case Operations.SAME:
			return new Bool(firstOperand == secondOperand);
		default:
			return new Text("" + firstOperand + secondOperand);
		}
	}
	private static DataInterface getResult(	double firstOperand, 
											double secondOperand, int operator) {
		switch (operator) {
		case Operations.ADDITION:
			return new DoubleFractional(firstOperand + secondOperand);
		case Operations.SUBTRACTION:
			return new DoubleFractional(firstOperand - secondOperand);
		case Operations.MULTIPLICATION:
			return new DoubleFractional(firstOperand * secondOperand);
		case Operations.DIVISION:
			return new DoubleFractional(firstOperand / secondOperand);
		case Operations.MORE:
			return new Bool(firstOperand > secondOperand);
		case Operations.MORE_OR_SAME:
			return new Bool(firstOperand >= secondOperand);
		case Operations.LESS:
			return new Bool(firstOperand < secondOperand);
		case Operations.LESS_OR_SAME:
			return new Bool(firstOperand <= secondOperand);
		case Operations.SAME:
			return new Bool(firstOperand == secondOperand);
		default:
			return new Text("" + firstOperand + secondOperand);
		}
	}
	private static DataInterface getResult(	boolean firstOperand, 
											boolean secondOperand, int operator) {
		switch (operator) {
		case Operations.SAME:
			return new Bool(firstOperand == secondOperand);
		case Operations.AND:
			return new Bool(firstOperand && secondOperand);
		case Operations.OR:
			return new Bool(firstOperand || secondOperand);
		case Operations.XOR:
			return new Bool(firstOperand ^ secondOperand);
		default:
			return new Text("" + firstOperand + secondOperand);
		}
	}
		
}
