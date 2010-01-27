package edu.kit.aifb.orel.inferencing;

import java.util.ArrayList;

/**
 * Description of a rule of inference that is applied to modify the
 * content of predicates based on the content of other predicates.
 * @author Markus Krötzsch
 */
public class InferenceRuleDeclaration {
	protected String name;
	protected ArrayList<PredicateAtom> body;
	protected PredicateAtom head;
	protected boolean retract;
	
	public static InferenceRuleDeclaration buildFromString(String name, String rule) {
		PredicateAtom head;
		boolean retract = false;
		ArrayList<PredicateAtom> body = new ArrayList<PredicateAtom>();
		String[] parts = rule.split(":\\-");
		if (parts.length != 2) return null;
		parts[0] = parts[0].trim();
		if (parts[0].charAt(0) == '-') {
			retract = true;
			parts[0] = parts[0].substring(1);
		}
		head = PredicateAtom.buildFromString(parts[0]);
		String[] bodyatoms = parts[1].split("\\) *,"); // note: it's OK to eat some closing ')' here; PredicateAtom does not care
		for (int i=0; i<bodyatoms.length; i++) {
			body.add(PredicateAtom.buildFromString(bodyatoms[i].trim()));
		}
		return new InferenceRuleDeclaration(name,body,head,retract);
	}

	public InferenceRuleDeclaration(String name, ArrayList<PredicateAtom> body, PredicateAtom head, boolean retract) {
		this.name = name;
		this.body = body;
		this.head = head;
		this.retract = retract;
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
	
	public boolean retractInferences() {
		return retract;
	}
}
