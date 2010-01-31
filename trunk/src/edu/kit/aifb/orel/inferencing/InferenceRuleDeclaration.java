package edu.kit.aifb.orel.inferencing;

import java.util.ArrayList;

/**
 * Description of a rule of inference that is applied to modify the
 * content of predicates based on the content of other predicates.
 * @author Markus Krötzsch
 */
public class InferenceRuleDeclaration {
	final static public int MODE_INFER = 1; // rule for standard inferences, leading to new assertions 
	final static public int MODE_RETRACT = 2; // rule for negative inferences, leading to retractions
	final static public int MODE_CHECK = 3; // rule to be used when checking for some predicate value, typically implementing trivial completions 
	
	protected String name;
	protected ArrayList<PredicateAtom> body;
	protected PredicateAtom head;
	protected int mode;
	
	public static InferenceRuleDeclaration buildFromString(String name, String rule) {
		PredicateAtom head;
		int mode;
		ArrayList<PredicateAtom> body = new ArrayList<PredicateAtom>();
		String[] parts = rule.split(":\\-");
		if (parts.length != 2) return null;
		parts[0] = parts[0].trim();
		if (parts[0].charAt(0) == '-') {
			mode = InferenceRuleDeclaration.MODE_RETRACT;
			parts[0] = parts[0].substring(1);
		} else if (parts[0].charAt(0) == '?') {
			mode = InferenceRuleDeclaration.MODE_CHECK;
			parts[0] = parts[0].substring(1);
		} else {
			mode = InferenceRuleDeclaration.MODE_INFER;
		}
		head = PredicateAtom.buildFromString(parts[0]);
		String[] bodyatoms = parts[1].split("\\) *,"); // note: it's OK to eat some closing ')' here; PredicateAtom does not care
		for (int i=0; i<bodyatoms.length; i++) {
			body.add(PredicateAtom.buildFromString(bodyatoms[i].trim()));
		}
		return new InferenceRuleDeclaration(name,body,head,mode);
	}

	public InferenceRuleDeclaration(String name, ArrayList<PredicateAtom> body, PredicateAtom head, int mode) {
		this.name = name;
		this.body = body;
		this.head = head;
		this.mode = mode;
	}
	
	public String getName() {
		return name;
	}
	
	public ArrayList<PredicateAtom> getBody() {
		return body;
	}
	
	public PredicateAtom getHead() {
		return head;
	}
	
	public int getMode() {
		return mode;
	}
}
