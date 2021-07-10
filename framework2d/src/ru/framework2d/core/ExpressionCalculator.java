package ru.framework2d.core;

import java.util.ArrayList;

import android.util.Log;

public class ExpressionCalculator {
	
	private static final String LOG_TAG = "Parser";
	
	private static interface Weights {
		final static int ADDITION = 0;
		final static int MULTIPLICATION = 1;
		final static int PARANTHESES = 2;
	}
	
	private static interface Operations {
		final static int ADDITION = 0;
		final static int SUBTRACTION = 1;
		final static int MULTIPLICATION = 2;
		final static int DIVISION = 3;
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
		public double value;
		public int operator;
		public void calculate() {
			if (!calculated) {
				if (!left.calculated) left.calculate();
				if (!right.calculated) right.calculate();
				calculated = true;
				switch (operator) {
					case Operations.ADDITION:
						value = left.value + right.value;
					break;
					case Operations.SUBTRACTION:
						value = left.value - right.value;
					break;
					case Operations.MULTIPLICATION:
						value = left.value * right.value;
					break;
					case Operations.DIVISION:
						value = left.value / right.value;
					break;
					default:
						calculated = false;
					break;
				}
			}
		}
		BinaryNode(){}
	}
	ExpressionCalculator() {};
	private static BinaryNode readNode(int start, int end, ArrayList <Operation> lstOperations, ArrayList <Double> lstValues){
		BinaryNode node = new ExpressionCalculator.BinaryNode();
		if (start == end) {
			node.calculated = true;
			switch (lstOperations.get(start).operator) {
				case Operations.ADDITION:
					node.value = lstValues.get(start) + lstValues.get(start + 1);
				break;
				case Operations.SUBTRACTION:
					node.value = lstValues.get(start) - lstValues.get(start + 1);
				break;
				case Operations.MULTIPLICATION:
					node.value = lstValues.get(start) * lstValues.get(start + 1);
				break;
				case Operations.DIVISION:
					node.value = lstValues.get(start) / lstValues.get(start + 1);
				break;
				default:
					node.calculated = false;
				break;
			}
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
			node.left = readNode(start, currentNo - 1, lstOperations, lstValues);
			node.right = readNode(currentNo + 1, end, lstOperations, lstValues);
		} else {
			node.value = lstValues.get(start);
			node.calculated = true;
		}
		return node;
	}
	
	private static ArrayList <Operation> lstOperations = new ArrayList <Operation>();
	private static ArrayList <Double> lstOperands = new ArrayList <Double>();
	private static String operand = "";
	
	public static String calculate(String expression) {
		
		boolean isExpression = true;
		
		if (expression.length() > 1 && expression.charAt(0) == '%' && expression.charAt(1) == 's') {
			return expression.substring(2);
		}
		
		expression.replaceAll(" ", "");
		operand = "";
		
		int paranthesesNum = 0;
		int ch = 0;
		char character;
		while (ch < expression.length() && isExpression) {
			character = expression.charAt(ch);
			
			if (Character.isLetter(character) || (character == '#')) {
				isExpression = false;
			} else if (Character.isDigit(character) || character == '.') {
				operand = operand + character;
			}
			
			if (character == '(') {
				paranthesesNum++;
			} else if (character == ')') {
				paranthesesNum--;
			} else if (character == '+') {
				lstOperations.add(new Operation(paranthesesNum * Weights.PARANTHESES + Weights.ADDITION, Operations.ADDITION));
				if (operand.length() > 0) lstOperands.add(Double.parseDouble(operand));
				operand = "";
			} else if(character == '-') {
				if (ch > 0) {
					if (expression.charAt(ch - 1) == '(') {
						operand = "-";
					} else {
						lstOperations.add(new Operation(paranthesesNum * Weights.PARANTHESES + Weights.ADDITION, Operations.SUBTRACTION));
						if (operand.length() > 0) lstOperands.add(Double.parseDouble(operand));
						operand = "";
					}
				} else {
					operand = "-";
				}
			} else if (character == '*') {
				lstOperations.add(new Operation(paranthesesNum * Weights.PARANTHESES + Weights.MULTIPLICATION, Operations.MULTIPLICATION));
				if (operand.length() > 0) lstOperands.add(Double.parseDouble(operand));
				operand = "";
			} else if (character == '/') {
				lstOperations.add(new Operation(paranthesesNum * Weights.PARANTHESES + Weights.MULTIPLICATION, Operations.DIVISION));
				if (operand.length() > 0) lstOperands.add(Double.parseDouble(operand));
				operand = "";
			}
			ch++;
		}
		if (operand.length() > 0) lstOperands.add(Double.parseDouble(operand));
		operand = "";
		/*for (int i=0; i<lstValues.size(); i++) {
			Log.d(TAG, "Value list: "+i+" got values = "+ lstValues.get(i)+";");
		}
		for (int i=0; i<lstOperations.size(); i++) {
			Log.d(TAG, "operations list: "+i+" got operation = "+ lstOperations.get(i).opnum+"; and ves = "+lstOperations.get(i).ves+";");
		}*/
		BinaryNode head;
		if (isExpression) {
			if (paranthesesNum == 0) {
				
				if (!lstOperations.isEmpty()){
					head = readNode(0, lstOperations.size() - 1, lstOperations, lstOperands);
					head.calculate();
					lstOperations.clear();
					lstOperands.clear();
				} else {
					head = new BinaryNode();
					head.value = lstOperands.get(0);
					lstOperands.clear();
				}
				
			} else {
				
				Log.d(LOG_TAG, "That is not valid expression: <" + expression + ">");
				lstOperations.clear();
				lstOperands.clear();
				return expression;
				
			}
		} else {
			//Log.d(LOG_TAG, "string is not expression: <"+s+">");
			lstOperations.clear();
			lstOperands.clear();
			return expression;
		}
		return "" + head.value;
	}
}
