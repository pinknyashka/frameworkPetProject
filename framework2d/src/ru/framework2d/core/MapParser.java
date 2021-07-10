package ru.framework2d.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import ru.framework2d.data.Bool;
import ru.framework2d.data.Fractional;
import ru.framework2d.data.Numeral;
import ru.framework2d.data.Text;


import android.util.Log;

public class MapParser {

    private static final String LOG_TAG = "Parser";

    private EntityStorage generatedStorage;	//result of map parsing
    
    private EnginesManager enginesManager;
    
    private Timer taskTimer; 
    
	public void parallelParseMap(XmlPullParser xpparser) {
		int delay = 0;
		taskTimer.schedule(new ParseTask(xpparser), delay);
	}
	
	class ParseTask extends TimerTask {
		
    	private XmlPullParser xpparser = null;
    	
		ParseTask(XmlPullParser xpparser) {
    		this.xpparser = xpparser;
    	}
    	
		@Override
	    public void run() {
	    	parseMap(xpparser);
	    	
	    	taskTimer.purge();
	    	taskTimer.cancel();
    	}
    }
   
	
    
    MapParser (EntityStorage objectManager, EnginesManager enginesManager, GameResources resources) {
    	
    	if (EnginesManager.PARALLEL_WORK) taskTimer = new Timer();
    	
    	this.generatedStorage = objectManager;
    	this.enginesManager = enginesManager;
    	
    	lstReplacements.add(
    			new Replacement(
    					"DisplayWidth", 
    					"" + resources.appResources.getDisplayMetrics().widthPixels
    					)
    			);
    	lstReplacements.add(
    			new Replacement(
    					"DisplayHeight", 
    					"" + resources.appResources.getDisplayMetrics().heightPixels
    					)
    			);
    }
    
    //WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWvv-Parsing-vvWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW
    
    public EntityStorage parseMap(XmlPullParser xpparser) {
    	
        Log.d(LOG_TAG, "Start map parsing...");
        
        Entity newEntity = null; //currently add entity

        long startParsingTimerMark = System.currentTimeMillis();
		long entityCreationTimerMark = System.currentTimeMillis();
        
	    try {
	    	while (xpparser.getEventType() != XmlPullParser.END_DOCUMENT) {
	    		
	    		switch (xpparser.getEventType()) {
	    		
	        		case XmlPullParser.START_DOCUMENT:
	        			break;
	        			
	        		case XmlPullParser.START_TAG:
	        			//===============================START_TAG=====================================
	        			
	        			long timerMark = System.currentTimeMillis();
	        			Component createdComponent = null;
	        			
	        			String tagName = xpparser.getName();
	        			
	        			//init----------------------------basic constructor----------------------------------
        				TagOptions tagOptions = new TagOptions();
        				tagOptions.name = tagName;
        				tagOptions.depth = xpparser.getDepth();
        				
        				for (int i = 0; i < xpparser.getAttributeCount(); i++) {
        					
        					String strValue = calculateExpression(xpparser.getAttributeValue(i));
        					
        					tagOptions.lstPropertys.add(
        							DataImplementator.toProperty(
        									xpparser.getAttributeName(i), 
        									replaceDefaults(xpparser.getAttributeValue(i))));
        					
        					//-------------------------------Log properties---------------------------------
        					Property lastProperty = tagOptions.lstPropertys.get(tagOptions.lstPropertys.size() - 1);
        					//SlastProperty.value = "" + strValue;
        					
        					String dataString = "";
        					if (lastProperty.data instanceof Fractional) {
        						dataString += "float: ";
        						dataString += lastProperty.data; 
        					} 
        					else if (lastProperty.data instanceof Numeral) {
        						dataString += "int: ";
        						dataString += lastProperty.data;
        					} 
        					else if (lastProperty.data instanceof Bool) {
        						dataString += "boolean: ";
        						dataString += lastProperty.data;
        					} 
        					else if (lastProperty.data instanceof Text) {
        						dataString += "string: ";
        						dataString += lastProperty.data;
        					}
        					
        					Log.d(LOG_TAG, "" + lastProperty.name + ": calculator: " + strValue + "; Implementator: " + dataString);
        					//-------------------------------Log properties---------------------------------
        					
        				}
        				
    					
	        			
	        			if (tagName.contentEquals("entity")) {
			        		//--------------------------------entity----------------------------------
        					Log.d(LOG_TAG, "Generating entity;");
        					
        					entityCreationTimerMark = System.currentTimeMillis();

	        				newEntity = generatedStorage.createEntity(tagOptions);
			        		//--------------------------------entity----------------------------------
	        			}
	        			else if (tagName.contentEquals("prototype")) {
			        		//--------------------------------prototype----------------------------------
	        				Log.d(LOG_TAG, "Generating prototype;");
	        				
        					entityCreationTimerMark = System.currentTimeMillis();
        					
	        				newEntity = generatedStorage.createPrototype(tagOptions);
			        		//--------------------------------prototype----------------------------------
	        			}
	        			else if (tagName.contentEquals("replace")) {
		        			//vvvv----------------------------replace------------------------------vvvv
	        				
	        				Replacement replace = new Replacement();
	        				
	        				for (int i = 0; i < xpparser.getAttributeCount(); i++) {
	        					
								if (xpparser.getAttributeName(i).contentEquals("name")) {
									
									replace.name = xpparser.getAttributeValue(i);
								}
								else if (xpparser.getAttributeName(i).contentEquals("value")) {
									
									replace.value = calculateExpression(xpparser.getAttributeValue(i));
								}
	        				}
	        				
	        				Property replacement = tagOptions.findPropertyByName("value");
	        				if (replacement != null) replace.value = replacement.data.toString();
	        				lstReplacements.add(replace);
		        			//^^^^^^----------------------------replace------------------------------^^^^^^
	        			}
	        			else if (tagName.contentEquals("context")) {
		        			//--------------------------------context----------------------------------
	        				
	        				enginesManager.setContext(tagOptions);
	        			}
	        			else if (tagName.contentEquals("handler") || 
	        					tagName.contentEquals("direct") ||
	        					tagName.contentEquals("sub") ||
	        					tagName.contentEquals("interaction")) {
	        				//-------------------handler+direct+sub+interaction-----------------------
	        				
	        				enginesManager.connectEngine(tagOptions);
	        			}
	        			else {
		        			//vvv---send------------------basic constructor--------------------------------------vvv
	        				createdComponent = enginesManager.createComponentInCurrentEntity(tagOptions);
		        			//^^^^--send-------------------basic constructor--------------------------------------^^^^
	        			}
	        			
	        			if (newEntity != null) {
		        			if (!newEntity.name.contentEquals("") && !newEntity.lstComponents.isEmpty() && createdComponent != null) { 
		        				Log.d(LOG_TAG, "Component <" + 
		        								newEntity.lstComponents.get(newEntity.lstComponents.size() - 1).getName() + 
		        								"> in entity <" + newEntity.name + "> generated in " + 
		        								(System.currentTimeMillis() - timerMark) + " ms;");
		        			}
	        			}
	        			
	        			break;
	        			
	        		//===============================END_TAG=====================================
	        		case XmlPullParser.END_TAG:
	        			String endTagName = xpparser.getName();

	        			if (endTagName.contentEquals("entity") || endTagName.contentEquals("prototype")) {
			        		//end_tag------------------------entity+prototype--------------------------------
	        				
	        				Log.d(LOG_TAG, endTagName + " " + newEntity.name + " generated in " + 
	        								(System.currentTimeMillis() - entityCreationTimerMark) + " ms;");
	        				
	        				generatedStorage.entityCreationIsOver();
	        				
	        				if (newEntity.parent != null) {
	        					newEntity = newEntity.parent;
	        				}
	        				else newEntity = null;
	        			}
	        			else if (endTagName.contentEquals("handler") || 
	        					endTagName.contentEquals("direct") ||
	        					endTagName.contentEquals("sub") ||
	        					endTagName.contentEquals("interaction")) {
		        			//end_tag-------------------handler+direct+sub+interaction-----------------------
	        				
	        				enginesManager.engineCreationIsOver();
	        			}
	        			else if (endTagName.contentEquals("map")) {
		        			//end_tag---------------------------map----------------------------------
	        				
		    				Log.d(LOG_TAG, "Map generated in " + 
		    								(System.currentTimeMillis() - startParsingTimerMark) 
		    								+ " ms; Created " + generatedStorage.lstEntities.size() 
		    								+ " entities;");
		    				
	        			} 
//	        			else if (!endTagName.contentEquals("context") &&
//	        					!endTagName.contentEquals("replace")) {
//	        				//end_tag---------------------------all other----------------------------------
//	        				
//	        				enginesManager.componentCreationIsOver();
//	        			}
	        			break;
	        			
	        		case XmlPullParser.TEXT:
	        			break;
	        			
	        		default:
	        			break;
	    		}
	    		xpparser.next();
	    	}        
	    } catch (XmlPullParserException e) {
	    	e.printStackTrace();
	    } catch (IOException e) {
	    	e.printStackTrace();
	    }
        Log.d(LOG_TAG, "Parsing is complete;");
        
		/* вывод созданных контекстов данных в логи
         * output created data contexts in logs
         * */
		/*for (DataContext dataContext : enginesManager.lstCommonContexts) {
			String tmp = "context: <" + dataContext.name + "> got linked components classes: \n";
			for (Class <Component> component : dataContext.lstLinkedComponents) {
				tmp += component.getSimpleName() + "; ";
			}
			tmp += "\n and next basic linked handlers: \n";
			for (ComponentsHandler componentsHandler : dataContext.lstBasicLinkedHandlers) {
				tmp += componentsHandler.name + "; ";
			}
			Log.d(LOG_TAG, tmp);
		}*/
        
	    return generatedStorage;
	}
    //MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM^^-Parsing-^^MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM

    private class Replacement {
    	String name;
    	String value;
    	Replacement() { }
    	Replacement(String name, String value) {
    		this.name = name;
    		this.value = value;
    	}
    }
    
    private String calculateExpression(String expression) {
    	expression = replaceDefaults(expression);
    	expression = ExpressionCalculator.calculate(expression);
    	return expression;
    }
    
	private ArrayList <Replacement> lstReplacements = new ArrayList <Replacement> ();

    private String replaceDefaults(String stringToReplace) {
    	for (int i = 0; i < lstReplacements.size(); i++) {
        	stringToReplace = stringToReplace.replaceAll(
        			lstReplacements.get(i).name, lstReplacements.get(i).value);
    	}
    	return stringToReplace;
    }
    
}
