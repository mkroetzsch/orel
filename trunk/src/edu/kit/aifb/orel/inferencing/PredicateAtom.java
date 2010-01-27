package edu.kit.aifb.orel.inferencing;

import java.util.ArrayList;

/**
 * Class for representing an atom in a rule of inference.
 * @author Markus Krötzsch
 */
public class PredicateAtom {
	protected String name;
	protected ArrayList<PredicateTerm> arguments;
	
	public static PredicateAtom buildFromString(String atom) {
		ArrayList<PredicateTerm> arguments = new ArrayList<PredicateTerm>();
		String[] parts = atom.split("[(,)]");
		if (parts.length <= 0) return null;
		String arg;
		for (int i=1; i<parts.length; i++) {
			arg = parts[i].trim();
			if (arg.charAt(0) == '"') { // treat strings as constant but remove enclosing '"'
				arguments.add(new PredicateTerm(arg.substring(1,arg.length()-2),false));
			} else if ( (arg.charAt(0) == '0') || (arg.charAt(0) == '1') || (arg.charAt(0) == '2') ||
					    (arg.charAt(0) == '3') || (arg.charAt(0) == '4') || (arg.charAt(0) == '5') ||
					    (arg.charAt(0) == '6') || (arg.charAt(0) == '7') || (arg.charAt(0) == '8') ||
					    (arg.charAt(0) == '9') ) { // treat numbers as constants
				arguments.add(new PredicateTerm(arg,false));
			} else { // treat as variable
				arguments.add(new PredicateTerm(arg,true));
			}
		}
		return new PredicateAtom(parts[0],arguments);
	}
	
	public PredicateAtom(String name, ArrayList<PredicateTerm> arguments) {
		this.name = name;
		this.arguments = arguments;
	}
	
	public String getName() {
		return name;
	}
	
	public ArrayList<PredicateTerm> getArguments() {
		return arguments;
	}
}
