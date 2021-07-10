package ru.framework2d.core;

import java.lang.reflect.Field;
import java.util.ArrayList;

import ru.framework2d.data.Bool;
import ru.framework2d.data.DataInterface;
import ru.framework2d.data.Fractional;


import android.util.Log;

public class Expression {
	
	private static final String LOG_TAG = "Parser";

	String fullString = "";
	
	BinaryNode primaryNode;
	
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
	
	EntityStorage objectModel;
	public Expression(EntityStorage objectModel) {
		this.objectModel = objectModel;
	}
	
	private class Operation {
		
		int weight; 
		int operator;
		
		Operation(int weight, int operator) {
			this.weight = weight;
			this.operator = operator;
		}
		
		public Variable calculate(Variable variable, Variable variable2) {
			
			if (variable.type == Variable.UNREACHABLE_DATA) {
				variable = getDataVariable(variable.fullValue);
			}
			if (variable2.type == Variable.UNREACHABLE_DATA) {
				variable2 = getDataVariable(variable2.fullValue);
			}
			if (variable.type != Variable.UNREACHABLE_DATA && variable2.type != Variable.UNREACHABLE_DATA) {
				if (variable.type == Variable.DATA) {
					if (variable.data instanceof Bool) {
						variable.d_value = (((Bool) variable.data).value)? 1.0: 0.0;
						variable.b_value = ((Bool) variable.data).value;
					} else if (variable.data instanceof Fractional) {
						variable.d_value = ((Fractional) variable.data).value;
						variable.f_value = ((Fractional) variable.data).value;
					}
				}
				if (variable2.type == Variable.DATA) {
					if (variable2.data instanceof Bool) {
						variable2.d_value = (((Bool) variable2.data).value)? 1.0: 0.0;
						variable2.b_value = ((Bool) variable2.data).value;
					} else if (variable2.data instanceof Fractional) {
						variable2.d_value = ((Fractional) variable2.data).value;
						variable2.f_value = ((Fractional) variable2.data).value;
					}
				}
				
				Variable result;
				switch (operator) {
					case Operations.ADDITION:
						result = new Variable(variable.d_value + variable2.d_value);
					break;
					case Operations.SUBTRACTION:
						result = new Variable(variable.d_value - variable2.d_value);
					break;
					case Operations.MULTIPLICATION:
						result = new Variable(variable.d_value * variable2.d_value);
					break;
					case Operations.DIVISION:
						result = new Variable(variable.d_value / variable2.d_value);
					break;
					case Operations.MORE:
						result = new Variable((variable.d_value > variable2.d_value)? true: false);
					break;
					case Operations.MORE_OR_SAME:
						result = new Variable((variable.d_value >= variable2.d_value)? true: false);
					break;
					case Operations.LESS:
						result = new Variable((variable.d_value < variable2.d_value)? true: false);
					break;
					case Operations.LESS_OR_SAME:
						result = new Variable((variable.d_value <= variable2.d_value)? true: false);
					break;
					case Operations.SAME:
						result = new Variable((variable.d_value == variable2.d_value)? true: false);
					break;
					case Operations.AND:
						result = new Variable((variable.d_value == 1 && variable2.d_value == 1)? true: false);
					break;
					case Operations.OR:
						result = new Variable((variable.d_value == 1 || variable2.d_value == 1)? true: false);
					break;
					default:
						result = null;
					break;
				}
				return result;
			}
			return new Variable(Variable.UNREACHABLE_DATA);
		}
		
	}
	
	private class BinaryNode {
		public BinaryNode left;
		public BinaryNode right;
		public boolean calculated = false;
		
		public boolean containsDataReferens = false;
		public boolean containsUnreachableDataReferens = false;
		
		public Variable variable;
		
		public Operation operation;

		public boolean calculate() {
			if (!calculated) {
				if (left != null && right != null) {
					if (!left.calculated) {
						if (left.calculate()) {
							if (left.containsDataReferens) containsDataReferens = true;
						} else {
							containsUnreachableDataReferens = true;
							return false;
						}
					}
					if (!right.calculated) {
						if (right.calculate()) {
							if (right.containsDataReferens) containsDataReferens = true;
						}else {
							containsUnreachableDataReferens = true;
							return false;
						}
					}
					variable = operation.calculate(left.variable, right.variable);
					if (variable.type != Variable.UNREACHABLE_DATA) {
						containsUnreachableDataReferens = false;
						if (!containsDataReferens) calculated = true;
					} else {
						containsUnreachableDataReferens = true;
						return false;
					}
				} else {
					if (variable.type == Variable.UNREACHABLE_DATA) {
						Log.d(LOG_TAG, "Expression: unreachable data while calculate: " + variable.fullValue);
						variable = getDataVariable(variable.fullValue);
					}
					if (variable.type != Variable.UNREACHABLE_DATA) {
						if (variable.type == Variable.DATA) {
							if (variable.data instanceof Bool) {
								variable.d_value = (((Bool) variable.data).value)? 1.0: 0.0;
								variable.b_value = ((Bool) variable.data).value;
							} else if (variable.data instanceof Fractional) {
								variable.d_value = ((Fractional) variable.data).value;
								variable.f_value = ((Fractional) variable.data).value;
							}
						}
					} 
					else return false;
				}
			} 
			return true;
		}
		BinaryNode() { }
	}
	private BinaryNode readNode(int start, int end, 
			ArrayList <Operation> lstOperations, ArrayList <Variable> lstVariables) {
		
		BinaryNode node = new Expression.BinaryNode();
		if (start == end) {
			if (lstVariables.get(start).type == Variable.DATA 
					|| lstVariables.get(start + 1).type == Variable.DATA) {
				node.variable = lstOperations.get(start).calculate(
						lstVariables.get(start), 
						lstVariables.get(start + 1));
				node.operation = lstOperations.get(start);
				node.containsDataReferens = true;
				node.left = readNode(start, start - 1, lstOperations, lstVariables);
				node.right = readNode(start + 1, start, lstOperations, lstVariables);
				if (node.left.containsDataReferens || node.right.containsDataReferens) {
					node.containsDataReferens = true; 
				}
				if (node.left.containsUnreachableDataReferens 
						|| node.right.containsUnreachableDataReferens) {
					node.containsUnreachableDataReferens = true;
				}
			} else if (lstVariables.get(start).type == Variable.UNREACHABLE_DATA 
					|| lstVariables.get(start + 1).type == Variable.UNREACHABLE_DATA) {
				node.variable = lstOperations.get(start).calculate(
						lstVariables.get(start), 
						lstVariables.get(start + 1));
				node.operation = lstOperations.get(start);
				node.containsDataReferens = true;
				node.containsUnreachableDataReferens = true;
				node.left = readNode(start, start - 1, lstOperations, lstVariables);
				node.right = readNode(start + 1, start, lstOperations, lstVariables);
				if (node.left.containsDataReferens || node.right.containsDataReferens) {
					node.containsDataReferens = true; 
				}
				if (node.left.containsUnreachableDataReferens 
						|| node.right.containsUnreachableDataReferens) {
					node.containsUnreachableDataReferens = true;
				}
			} else {
				node.calculated = true;
				node.variable = lstOperations.get(start).calculate(
						lstVariables.get(start), 
						lstVariables.get(start + 1));
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
			node.operation = lstOperations.get(currentNo);
			node.left = readNode(start, currentNo - 1, lstOperations, lstVariables);
			node.right = readNode(currentNo + 1, end, lstOperations, lstVariables);
			if (node.left.containsDataReferens || node.right.containsDataReferens) {
				node.containsDataReferens = true; 
			}
			if (node.left.containsUnreachableDataReferens 
					|| node.right.containsUnreachableDataReferens) {
				node.containsUnreachableDataReferens = true;
			}
		} else {
			node.variable = lstVariables.get(start);
			
			if (node.variable.type == Variable.DATA) {
				node.containsDataReferens = true;
			} else if (node.variable.type == Variable.UNREACHABLE_DATA) {
				node.containsDataReferens = true;
				node.containsUnreachableDataReferens = true;
			} else {
				node.calculated = true;
			}
		}
		return node;
	}
	
	boolean isExpression = true;
	boolean isValidExpression = true;
	
	public double calculate() {
		if (isExpression && isValidExpression) {
			if (primaryNode.containsDataReferens) {
				if (primaryNode.calculate()) {
					Log.d(LOG_TAG, "Expression: string: <" 
							+ fullString + ">; value: <" + primaryNode.variable.d_value + ">");
					return primaryNode.variable.d_value;
				}
			}
			else return primaryNode.variable.d_value;
		}
		return 0;
	}
	
	ArrayList <Operation> lstOperations = new ArrayList <Operation>();
	ArrayList <Variable> lstVariables = new ArrayList <Variable>();
	String strVariable = "";
	
	public boolean setExpression(DataInterface thisData, String expression) {
		
		Log.d(LOG_TAG, "Expression: expression to set: <" + expression + ">");
		
		boolean isEquation = false;
		boolean readingPointer = false;
		
		fullString = expression;
		
		expression.replaceAll(" ", "");
		strVariable = "";
		
		int paranthesesLevel = 0;
		int ch = 0;
		int length = expression.length();
		while (ch < length && isExpression) {
			char character = expression.charAt(ch);
			boolean isLetter = Character.isLetter(character);
			if (character == '$') {
				isEquation = true;
				readingPointer = true;
			}
			if (isLetter) {
				boolean gotBoolean = false;
				if (character == 't') {
					if (expression.substring(ch, ch + 4).contentEquals("true")) {
						if (strVariable.length() == 0) {
							strVariable = "true";
							lstVariables.add(new Variable(strVariable, Variable.BOOLEAN));
							gotBoolean = true;
						}
						ch += 3;
					} 
				} else if (character == 'f') {
					if (expression.substring(ch, ch + 5).contentEquals("false")) {
						if (strVariable.length() == 0) {
							strVariable = "false";
							lstVariables.add(new Variable(strVariable, Variable.BOOLEAN));
							gotBoolean = true;
						}
						ch += 4;
					} 
				}
				if (!isEquation && !gotBoolean) isExpression = false;
			}
			if ( ( Character.isDigit(character) || (isEquation && readingPointer && isLetter) ) 
					|| character == '.' || character == ':') {
				strVariable = strVariable + character;
			}
			
			paranthesesLevel += (character == '(')? 1 : (character==')')? -1 : 0;
			if (paranthesesLevel < 0) isValidExpression = false;
			
			if (character == '+') {
				lstOperations.add(new Operation(
						paranthesesLevel * Weights.PARANTHESES + Weights.ADDITION, 
						Operations.ADDITION));
				if (strVariable.length() > 0) {
					if (readingPointer) lstVariables.add(new Variable(strVariable, Variable.DATA));
					else lstVariables.add(new Variable(strVariable, Variable.UNKNOWN));
				}
				strVariable = "";
				readingPointer = false;
			} else if(character == '-') {
				if (ch > 0){
					if (expression.charAt(ch - 1) == '(') {
						strVariable = "-";
					} else {
						lstOperations.add(new Operation(
								paranthesesLevel * Weights.PARANTHESES + Weights.ADDITION, 
								Operations.SUBTRACTION));
						if (strVariable.length() > 0) {
							if (readingPointer) lstVariables.add(new Variable(strVariable, Variable.DATA));
							else lstVariables.add(new Variable(strVariable, Variable.UNKNOWN));
						}
						strVariable = "";
						readingPointer = false;
					}
				} else {
					strVariable = "-";
				}
			} else if (character == '*') {
				lstOperations.add(new Operation(
						paranthesesLevel * Weights.PARANTHESES + Weights.MULTIPLICATION, 
						Operations.MULTIPLICATION));
				if (strVariable.length() > 0) {
					if (readingPointer) lstVariables.add(new Variable(strVariable, Variable.DATA));
					else lstVariables.add(new Variable(strVariable, Variable.UNKNOWN));
				}
				strVariable = "";
				readingPointer = false;
			} else if(character == '/') {
				lstOperations.add(new Operation(
						paranthesesLevel * Weights.PARANTHESES + Weights.MULTIPLICATION, 
						Operations.DIVISION));
				if (strVariable.length() > 0) {
					if (readingPointer) lstVariables.add(new Variable(strVariable, Variable.DATA));
					else lstVariables.add(new Variable(strVariable, Variable.UNKNOWN));
				}
				strVariable = "";
				readingPointer = false;
			} else if(character == '>') {
				if (expression.length() > ch + 1 && expression.charAt(ch + 1) == '=') {
					lstOperations.add(new Operation(
							paranthesesLevel * Weights.PARANTHESES + Weights.BOOLEAN, 
							Operations.MORE_OR_SAME));
					if (strVariable.length() > 0) {
						if (readingPointer) lstVariables.add(new Variable(strVariable, Variable.DATA));
						else lstVariables.add(new Variable(strVariable, Variable.UNKNOWN));
					}
					strVariable = "";
					readingPointer = false;
					ch++;
				} else {
					lstOperations.add(new Operation(
							paranthesesLevel * Weights.PARANTHESES + Weights.BOOLEAN, 
							Operations.MORE));
					if (strVariable.length() > 0) {
						if (readingPointer) lstVariables.add(new Variable(strVariable, Variable.DATA));
						else lstVariables.add(new Variable(strVariable, Variable.UNKNOWN));
					}
					strVariable = "";
					readingPointer = false;
				}
			} else if(character == '&' && expression.substring(ch, ch + 5).contentEquals("false")) {
				Log.e("Expression", "here " + expression);
				if (expression.length() > ch + 1 && expression.charAt(ch + 1) == '=') {
					lstOperations.add(new Operation(
							paranthesesLevel * Weights.PARANTHESES + Weights.BOOLEAN, 
							Operations.LESS_OR_SAME));
					if (strVariable.length() > 0) {
						if (readingPointer) lstVariables.add(new Variable(strVariable, Variable.DATA));
						else lstVariables.add(new Variable(strVariable, Variable.UNKNOWN));
					}
					strVariable = "";
					readingPointer = false;
					ch++;
				} else {
					lstOperations.add(new Operation(
							paranthesesLevel * Weights.PARANTHESES + Weights.BOOLEAN, 
							Operations.LESS));
					if (strVariable.length() > 0) {
						if (readingPointer) lstVariables.add(new Variable(strVariable, Variable.DATA));
						else lstVariables.add(new Variable(strVariable, Variable.UNKNOWN));
					}
					strVariable = "";
					readingPointer = false;
				}
			} else if(character == '=') {
				lstOperations.add(new Operation(
						paranthesesLevel * Weights.PARANTHESES + Weights.BOOLEAN, 
						Operations.SAME));
				if (strVariable.length() > 0) {
					if (readingPointer) lstVariables.add(new Variable(strVariable, Variable.DATA));
					else lstVariables.add(new Variable(strVariable, Variable.UNKNOWN));
				}
				strVariable = "";
				readingPointer = false;
				if (expression.length() > ch + 1 && expression.charAt(ch + 1) == '=') ch++;
			} else if(character == 'A') {
				if (expression.length() > ch + 2 && expression.charAt(ch + 1) == 'N' && expression.charAt(ch + 2) == 'D') {
					lstOperations.add(new Operation(paranthesesLevel * Weights.PARANTHESES + Weights.BOOLEAN_OR_AND, Operations.AND));
					if (strVariable.length() > 0) {
						if (readingPointer) lstVariables.add(new Variable(strVariable, Variable.DATA));
						else lstVariables.add(new Variable(strVariable, Variable.UNKNOWN));
					}
					strVariable = "";
					readingPointer = false;
					ch += 2;
				}
			} else if(character == 'O') {
				if (expression.length() > ch + 1 && expression.charAt(ch + 1) == 'R') {
					lstOperations.add(new Operation(paranthesesLevel * Weights.PARANTHESES + Weights.BOOLEAN_OR_AND, Operations.OR));
					if (strVariable.length() > 0) {
						if (readingPointer) lstVariables.add(new Variable(strVariable, Variable.DATA));
						else lstVariables.add(new Variable(strVariable, Variable.UNKNOWN));
					}
					strVariable = "";
					readingPointer = false;
					ch++;
				}
			} 
			ch++;
		}
		if (strVariable.length() > 0) {
			if (readingPointer) lstVariables.add(new Variable(strVariable, Variable.DATA));
			else lstVariables.add(new Variable(strVariable, Variable.UNKNOWN));
		}
		strVariable = "";
		/*for (int i=0; i<lstValues.size(); i++) {
			Log.d(TAG, "Value list: "+i+" got values = "+ lstValues.get(i)+";");
		}
		for (int i=0; i<lstOperations.size(); i++) {
			Log.d(TAG, "operations list: "+i+" got operation = "+ lstOperations.get(i).opnum+"; and ves = "+lstOperations.get(i).ves+";");
		}*/
		if (paranthesesLevel != 0 || !isValidExpression) {
			isExpression = false;
			Log.d(LOG_TAG, "Expression: This is not valid expression: <" + expression + ">");
		}
		if (!isExpression) {
			lstOperations.clear();
			lstVariables.clear();
			return false;	
		} else {
			if (!lstOperations.isEmpty()) {
				primaryNode = readNode(0, lstOperations.size() - 1, lstOperations, lstVariables);
				if (primaryNode.calculate()) {
					Log.d(LOG_TAG, "Expression: Result = " + primaryNode.variable.d_value + "; ");
				} else if (primaryNode.containsUnreachableDataReferens) {
					String tmpLog = "";
					for (Variable variable: lstVariables) {
						if (variable.type == Variable.UNREACHABLE_DATA) {
							tmpLog += variable.fullValue + "; ";
						}
					}
					Log.d(LOG_TAG, "Expression: Got unreachable data referens(es): " + tmpLog);
				}
			} else {
				primaryNode = new BinaryNode();
				primaryNode.variable = lstVariables.get(0);
			}
			lstOperations.clear();
			lstVariables.clear();
			return true;
		}
		//return "" + primaryNode.value;
	}
	private class Variable {
		static final int UNKNOWN = 1;
		static final int DATA = 2;
		static final int DOUBLE = 3;
		static final int BOOLEAN = 4;
		static final int FRACTIONAL = 5;
		static final int STRING = 6;
		static final int UNREACHABLE_DATA = 7;
		
		int type = 0;
		
		Variable(int type) {this.type = type; }
		
		String fullValue = "";
		
		DataInterface data;
		Variable(DataInterface data, String fullString) {
			this.data = data; this.type = DATA; 
			if (fullString != null) this.fullValue = fullString;
			else fullString = data.getName();
		}
		
		String s_value;
		Variable(String string) {this.s_value = string; this.type = STRING; fullValue = string;}
		
		boolean b_value;
		Variable(boolean value) {
			this.b_value = value; 
			this.d_value = value? 1.0: 0.0; 
			this.type = BOOLEAN; 
			fullValue = "" + value;
		}
		
		float f_value;
		Variable(float value) {
			this.f_value = value; 
			this.d_value = value; 
			this.type = FRACTIONAL; 
			fullValue = "" + value;
		}
		
		double d_value;
		Variable(double value) {
			this.d_value = value; 
			this.type = DOUBLE; 
			fullValue = "" + value;
		}
		
		Variable(String value, int type) {
			fullValue = value; this.type = type;
			switch (type) {
				case DATA:
					Variable tmp = getDataVariable(value);
					if (tmp.data != null) data = tmp.data;
					else this.type = UNREACHABLE_DATA;
				break;
				case DOUBLE:
					this.d_value = Double.parseDouble(value);
					this.b_value = (this.d_value == 1)? true: false;
				break;
				case FRACTIONAL:
					this.d_value = Double.parseDouble(value);
					this.f_value = (float) this.d_value;
				break;
				case BOOLEAN:
					try {
						this.d_value = Double.parseDouble(value);
						this.b_value = (this.d_value == 1) ? true : false;
						if (!b_value) {
							if (this.d_value != 0) type = DOUBLE;
						}
					} catch (NumberFormatException E){
						this.b_value = (value.contentEquals("true")) ? true : false;
						this.d_value = (this.b_value)? 1.0 : 0.0; 
					}
				break;
				case STRING:
					this.s_value = value;
				break;
				case UNKNOWN:
					try {
						this.d_value = Double.parseDouble(value);
						this.type = DOUBLE;
					} catch (NumberFormatException E){
						Log.d(LOG_TAG, "Expression: catched exception with string: " + value);
						if (value.contentEquals("true")) {
							b_value = true;
							d_value = 1.0;
							this.type = BOOLEAN;
						} else if (value.contentEquals("false")) { 
							b_value = false;
							d_value = 0.0;
							this.type = BOOLEAN;
						} else {
							Variable possibleData = getDataVariable(value);
							if (possibleData != null){
								data = possibleData.data;
								this.type = DATA;
							} else {
								s_value = value;
								this.type = STRING;
							}
						} 
					}
				break;
				case UNREACHABLE_DATA:
					Log.d(LOG_TAG, "Expression: unreachable data: " + value);
				break;
				default:
					this.type = UNKNOWN;
				break;
			}
		}
		
	}
	
	private Variable getDataVariable(String string) {
		
		Log.d(LOG_TAG, "Expression: string to parse: <" + string + ">");
		
		if (string.length() > 0) {
			String objectName = string.substring(0, string.indexOf('.'));
			Log.d(LOG_TAG, "Expression: object name = " + objectName);
			for (Entity object : objectModel.lstEntities) {
				if (objectName.contentEquals(object.name)) {
					
					String componentName = string.substring(objectName.length() + 1, 
							string.substring(objectName.length() + 1).indexOf('.') + objectName.length() + 1);
					
					Log.d(LOG_TAG, "Expression: component name = " + componentName);
					
					for (Component component : object.lstComponents) {
						if (componentName.contains(component.getName())) {
							/*String dataName = string.substring(objectName.length() + componentName.length() + 2, 
									string.substring(objectName.length() + componentName.length() + 2).indexOf('.') 
									+ objectName.length()) + componentName.length() + 2;*/
							String dataName = string.substring(objectName.length() + componentName.length() + 2);
							Log.d(LOG_TAG, "Expression: data name = " + dataName + ";");
							String currentDataName = "" + dataName;
							if (dataName.contains(":")) currentDataName = dataName.substring(0, dataName.indexOf(':'));
							DataInterface currentData = null;
							Class <Component> superComponent = (Class <Component>) component.getClass();
							while (Component.class.isAssignableFrom(superComponent)) {
								for (Field field: superComponent.getDeclaredFields()) {
									if (field.getName().contentEquals(currentDataName)) {
										try {
											DataInterface data = (DataInterface) field.get(component);
											Log.d(LOG_TAG, "Expression: got Variable " + data.getName() + ";");
											if (!dataName.contains(":")) return new Variable(data, string);
											else currentData = data;
										} catch (IllegalArgumentException e) {
											Log.e(LOG_TAG, "Expression: IllegalArgumentException");
											e.printStackTrace();
										} catch (IllegalAccessException e) {
											Log.e(LOG_TAG, "Expression: IllegalAccessException");
											e.printStackTrace();
										}
									}								
								}
								superComponent = (Class <Component>) superComponent.getSuperclass();
							}
							while (dataName.contains(":")) {
								
								dataName = dataName.substring(dataName.indexOf(':') + 1);
								
								if (!dataName.contains(":")) currentDataName = "" + dataName;
								else currentDataName = dataName.substring(0, dataName.indexOf(':'));
								Class <DataInterface> superData = (Class <DataInterface>) currentData.getClass();
								while (DataInterface.class.isAssignableFrom(superData)) {
									for (Field field: superData.getDeclaredFields()) {
										if (field.getName().contentEquals(currentDataName)) {
											try {
												Log.d(LOG_TAG, "Expression: got Variable: " + currentData.getName() + ";");
												currentData = (DataInterface) field.get(currentData);
											} catch (IllegalArgumentException e) {
												currentData = null;
												e.printStackTrace();
											} catch (IllegalAccessException e) {
												currentData = null;
												e.printStackTrace();
											}
										}								
									}
									superData = (Class <DataInterface>) superData.getSuperclass();
								}
							}	
							if (currentData != null) {
								Log.d(LOG_TAG, "Expression: got Variable " + currentData.getName() + ";");
								return new Variable(currentData, string);
							}
						}
					}
				}
			}
		}
		return new Variable(string, Variable.UNREACHABLE_DATA);
	}	
}

