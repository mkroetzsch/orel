package edu.kit.aifb.orel.test;


import org.semanticweb.owlapi.apibinding.*;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.*;

import edu.kit.aifb.orel.kbmanager.BasicKBManager;
import edu.kit.aifb.orel.kbmanager.BasicKBManager.InferenceResult;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.Set;
import java.io.*;

/**
 * Class to load and execute OWL 2 test cases from an ontology document. 
 * @author Anees ul Medhi
 * @author Markus Krötzsch
 */
public class OWLWGTestCaseChecker {
	static final String TEST_NS = "http://www.w3.org/2007/OWL/testOntology#";
	static final String TEST_ONTOLOGY_STUB = "Ontology(<http://www.w3.org/2007/OWL/testOntology>)";
	static public enum TestResult { PASS,FAIL,INCOMPLETE };
	static public enum TestType { ENTAILMENT,NONENTAILMENT,CONSISTENCY,INCONSISTENCY,UNKNOWN };
	
	protected class TestCaseData {
		public TestType type;
		public String uri;
		public OWLOntology premiseOntology = null;
		public OWLOntology conclusionOntology = null;
		public OWLOntology nonConclusionOntology = null;
		public boolean directSemantics = false;
		
		public TestCaseData(String uri, TestType type) {
			this.uri = uri;
			this.type = type;
		}
		
		public boolean relevantToOrel() {
			return directSemantics && 
			       (premiseOntology != null) &&
			       ( (type==TestType.CONSISTENCY) || 
			         (type==TestType.INCONSISTENCY) || 
			         ( (type==TestType.ENTAILMENT) && (conclusionOntology != null) ) ||
			         ( (type==TestType.NONENTAILMENT) && (nonConclusionOntology != null) )
			       );
		}
	}

	protected OWLOntologyManager manager;
	protected OWLOntology testCaseOntology;
	protected BufferedWriter outputbuffer;
	
	BasicKBManager kbmanager;
	
	/**
	 * @param file (String) containing the ontology test 
	 */
	public OWLWGTestCaseChecker(IRI testcases,BasicKBManager kbmanager) throws OWLOntologyCreationException, SQLException, IOException {
		this.kbmanager = kbmanager;
		manager = OWLManager.createOWLOntologyManager();
		manager.loadOntologyFromOntologyDocument(new StringDocumentSource(TEST_ONTOLOGY_STUB));
		testCaseOntology = manager.loadOntologyFromOntologyDocument(testcases);
	}
	
	public void runTests(String outputfile) throws Exception {
		outputbuffer = new BufferedWriter(new FileWriter(outputfile));
		Set<OWLClassAssertionAxiom> axiomset = testCaseOntology.getAxioms(AxiomType.CLASS_ASSERTION);
		Set<OWLLogicalAxiom> dataset = testCaseOntology.getLogicalAxioms();
		Iterator<OWLClassAssertionAxiom> classIterator = axiomset.iterator();
		OWLClassAssertionAxiom classAssert;
		TestCaseData testcase;
		boolean loaded;
		InferenceResult infResult,correctResult;
		while (classIterator.hasNext()) {
			kbmanager.drop();
			kbmanager.initialize();
			classAssert = classIterator.next();

			testcase = extractTestCaseData(dataset, classAssert);
			if (!testcase.relevantToOrel()) continue;
			System.out.println("Considering test case " + testcase.uri + " (" + testcase.type + ") ...");
			loaded = kbmanager.loadOntology(testcase.premiseOntology);
			if (testcase.type == TestType.CONSISTENCY) {
				infResult = negateInferenceResult(kbmanager.checkConsistency());
				correctResult = InferenceResult.NO;
			} else if (testcase.type == TestType.INCONSISTENCY) {
				infResult = negateInferenceResult(kbmanager.checkConsistency());
				correctResult = InferenceResult.YES;
			} else if (testcase.type == TestType.ENTAILMENT) {
				infResult = kbmanager.checkEntailment(testcase.conclusionOntology);
				correctResult = InferenceResult.YES;
			} else if (testcase.type == TestType.NONENTAILMENT) {
				infResult = kbmanager.checkEntailment(testcase.nonConclusionOntology);
				correctResult = InferenceResult.NO;
			} else {
				//outputbuffer.write("***Unsupported test type " + testuri + "\n");
				continue;
			}
			
			if ( (!loaded) && (infResult == InferenceResult.NO) ) {
				infResult = InferenceResult.DONTKNOW;
			}
			
			if (infResult == InferenceResult.DONTKNOW) {
				reportResult(TestResult.INCOMPLETE, testcase);
			} else if (infResult == correctResult) {
				reportResult(TestResult.PASS, testcase);
			} else {
				reportResult(TestResult.FAIL, testcase);
			}
			outputbuffer.flush();
		}
		outputbuffer.close();
	}

	protected TestCaseData extractTestCaseData(Set<OWLLogicalAxiom> dataset, OWLClassAssertionAxiom testClassAssertion) throws OWLOntologyCreationException{
		
		TestCaseData result;
		TestType testtype;
		String testclassuri = testClassAssertion.getClassExpression().toString();
		if (testclassuri.equals("<" + TEST_NS + "PositiveEntailmentTest>")) {
			testtype = TestType.ENTAILMENT;
		} else if (testclassuri.equals("<" + TEST_NS + "NegativeEntailmentTest>")) {
			testtype = TestType.NONENTAILMENT;
		} else if (testclassuri.equals("<" + TEST_NS + "ConsistencyTest>")) {
			testtype = TestType.CONSISTENCY;
		} else if (testclassuri.equals("<" + TEST_NS + "InconsistencyTest>")) {
			testtype = TestType.INCONSISTENCY;
		} else {
			testtype = TestType.UNKNOWN;
			return new TestCaseData(testClassAssertion.getIndividual().toString(),testtype);
		}
		
		result = new TestCaseData(testClassAssertion.getIndividual().toString(),testtype);
		OWLLogicalAxiom axiom;
		String propname;
		Iterator<OWLLogicalAxiom> dataIterator = dataset.iterator();
		while (dataIterator.hasNext()) {
			axiom = dataIterator.next();
			if (axiom instanceof OWLDataPropertyAssertionAxiom) {
				if (((OWLDataPropertyAssertionAxiom)axiom).getSubject().equals(testClassAssertion.getIndividual())) {
					propname = ((OWLDataPropertyAssertionAxiom)axiom).getProperty().toString();
					if ( (result.premiseOntology == null) && 
					     ( propname.equals("<" + TEST_NS + "rdfXmlPremiseOntology>") ||
					       propname.equals("<" + TEST_NS + "fsPremiseOntology>") ) ) {
						result.premiseOntology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(
								                  (OWLOntologyDocumentSource)new StringDocumentSource( ((OWLDataPropertyAssertionAxiom)axiom).getObject().getLiteral() )
								                 );
					} else if ( (result.conclusionOntology == null) && 
							    ( propname.equals("<" + TEST_NS + "rdfXmlConclusionOntology>") ||
							      propname.equals("<" + TEST_NS + "fsConclusionOntology>") ) ) {
						result.conclusionOntology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(
						                             (OWLOntologyDocumentSource)new StringDocumentSource(((OWLDataPropertyAssertionAxiom)axiom).getObject().getLiteral())
						                            );
					} else if ( (result.nonConclusionOntology == null) && 
							    ( propname.equals("<" + TEST_NS + "rdfXmlNonConclusionOntology>") ||
							      propname.equals("<" + TEST_NS + "fsNonConclusionOntology>") ) ) {
						result.nonConclusionOntology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(
						                                new StringDocumentSource(((OWLDataPropertyAssertionAxiom)axiom).getObject().getLiteral())
						                               );
					}
				}
			} else if (axiom instanceof OWLObjectPropertyAssertionAxiom) {
				if (((OWLObjectPropertyAssertionAxiom)axiom).getSubject().equals(testClassAssertion.getIndividual())) {
					propname = ((OWLObjectPropertyAssertionAxiom)axiom).getProperty().toString();
					if (propname.equals("<" + TEST_NS + "semantics>")) {
						if (((OWLObjectPropertyAssertionAxiom)axiom).getObject().toString().equals("<" + TEST_NS + "DIRECT>")) {
							result.directSemantics = true;
						}
					}
				}
			}
		}
		return result;
	}
	
	protected InferenceResult negateInferenceResult(InferenceResult result) {
		if (result == InferenceResult.NO) {
			return InferenceResult.YES;
		} else if (result == InferenceResult.YES) {
			return InferenceResult.NO;
		} else {
			return InferenceResult.DONTKNOW;
		}
	}
	
	protected void reportResult(TestResult result, TestCaseData testcase) {
		try {
			if ( result == TestResult.FAIL ) {
				outputbuffer.write("FAILED " + testcase.uri + " (" + testcase.type + ")\n");
			} else if ( result == TestResult.PASS ) {
				outputbuffer.write("   PASSED " + testcase.uri + " (" + testcase.type + ")\n");
			} else if ( result == TestResult.INCOMPLETE ) {
				outputbuffer.write("INCOMPLETE " + testcase.uri + " (" + testcase.type + ")\n");
			}
		} catch (IOException e) {
			System.err.println("Problem writing test case output: " + e.toString());
		}
	}
	
}	
