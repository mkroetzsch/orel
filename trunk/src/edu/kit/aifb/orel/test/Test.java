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
public class Test {
	static public enum TestResult { PASS,FAIL,INCOMPLETE };
	static public enum TestType { ENTAILMENT,NONENTAILMENT,CONSISTENCY,INCONSISTENCY,UNKNOWN };

	private OWLOntologyManager manager;
	private IRI physicalURI;
	private OWLOntology ontology;
	
	protected OWLOntology premiseOntology;
	protected OWLOntology conclusionOntology;
	protected OWLOntology nonConclusionOntology;
	
	protected BufferedWriter outputbuffer;
	
	
	/**
	 * @param file (String) containing the ontology test 
	 */
	public Test(String file) throws OWLOntologyCreationException, SQLException, IOException {
		loadOntology(file);
	}
	
	protected void loadOntology(String filename) throws OWLOntologyCreationException {
		manager = OWLManager.createOWLOntologyManager();
		physicalURI= IRI.create( (new File(System.getProperty("user.dir") + "/" +  filename)).toURI() );
		//		physicalURI= URI.create("http://owl.semanticweb.org/exports/proposed/RL-RDF-rules-tests.rdf");
		ontology = manager.loadOntologyFromOntologyDocument(physicalURI);
	}
	
	public OWLOntology getPremiseOntology() {
		return premiseOntology;
	}
	
	public OWLOntology getConclusionOntology() {
		return conclusionOntology;
	}

	public void test(BasicKBManager kbmanager) throws Exception {
		outputbuffer = new BufferedWriter(new FileWriter("testresults.txt"));
		Set<OWLClassAssertionAxiom> axiomset = ontology.getAxioms(AxiomType.CLASS_ASSERTION);
		Set<OWLDataPropertyAssertionAxiom> dataset = ontology.getAxioms(AxiomType.DATA_PROPERTY_ASSERTION);
		Iterator<OWLClassAssertionAxiom> classIterator = axiomset.iterator();
		OWLClassAssertionAxiom classAssert;
		String testuri, testclassuri;
		TestType testtype;
		boolean loaded;
		InferenceResult infResult,correctResult;
		
		while (classIterator.hasNext()) {
			kbmanager.drop();
			kbmanager.initialize();
			classAssert = classIterator.next();
			testuri = classAssert.getIndividual().toString();
			testclassuri = classAssert.getClassExpression().toString();
			if (testclassuri.equals("<http://www.w3.org/2007/OWL/testOntology#PositiveEntailmentTest>")) {
				testtype = TestType.ENTAILMENT;
			} else if (testclassuri.equals("<http://www.w3.org/2007/OWL/testOntology#NegativeEntailmentTest>")) {
				testtype = TestType.NONENTAILMENT;
			} else if (testclassuri.equals("<http://www.w3.org/2007/OWL/testOntology#ConsistencyTest>")) {
				testtype = TestType.CONSISTENCY;
			} else if (testclassuri.equals("<http://www.w3.org/2007/OWL/testOntology#InconsistencyTest>")) {
				testtype = TestType.INCONSISTENCY;
			} else {
				testtype = TestType.UNKNOWN;
				continue;
			}
			System.out.println("Considering test case " + testuri + " (" + testtype + ") ...");
			setTestCaseOntologies(dataset, classAssert.getIndividual());
			if (premiseOntology != null) {
				loaded = kbmanager.loadOntology(premiseOntology);
			} else {
				outputbuffer.write("*** Problem finding premise for test " + testuri + "\n");
				continue;
			}
			if (testtype == TestType.CONSISTENCY) {
				infResult = negateInferenceResult(kbmanager.checkConsistency());
				correctResult = InferenceResult.NO;
			} else if (testtype == TestType.INCONSISTENCY) {
				infResult = negateInferenceResult(kbmanager.checkConsistency());
				correctResult = InferenceResult.YES;
			} else if ( (testtype == TestType.ENTAILMENT) && (conclusionOntology != null) ) {
				infResult = kbmanager.checkEntailment(conclusionOntology);
				correctResult = InferenceResult.YES;
			} else if ( (testtype == TestType.NONENTAILMENT) && (nonConclusionOntology != null) ) {
				infResult = kbmanager.checkEntailment(nonConclusionOntology);
				correctResult = InferenceResult.NO;
			} else {
				//outputbuffer.write("***Unsupported test type " + testuri + "\n");
				continue;
			}
			
			if ( (!loaded) && (infResult == InferenceResult.NO) ) {
				infResult = InferenceResult.DONTKNOW;
			}
			
			if (infResult == InferenceResult.DONTKNOW) {
				reportResult(TestResult.INCOMPLETE, testuri, testtype);
			} else if (infResult == correctResult) {
				reportResult(TestResult.PASS, testuri, testtype);
			} else {
				reportResult(TestResult.FAIL, testuri, testtype);
			}
			outputbuffer.flush();
		}
		outputbuffer.close();
	}

	protected void setTestCaseOntologies(Set<OWLDataPropertyAssertionAxiom> dataset, OWLIndividual testcase) throws OWLOntologyCreationException{
		OWLDataPropertyAssertionAxiom dataAxiom;
		Iterator<OWLDataPropertyAssertionAxiom> dataIterator = dataset.iterator();
		premiseOntology = null;
		conclusionOntology = null;
		nonConclusionOntology = null;
		while (dataIterator.hasNext()) {
			dataAxiom = dataIterator.next();
			if (dataAxiom.getSubject().equals(testcase)) {
				if ( (premiseOntology == null) && 
				     ( dataAxiom.getProperty().toString().equals("<http://www.w3.org/2007/OWL/testOntology#rdfXmlPremiseOntology>") ||
				       dataAxiom.getProperty().toString().equals("<http://www.w3.org/2007/OWL/testOntology#fsPremiseOntology>") ) ) {
					//							System.out.println("PremiseOntology:");
					//							System.out.println(dataAxiom.getObject().getLiteral());
					premiseOntology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(
							            (OWLOntologyDocumentSource)new StringDocumentSource( dataAxiom.getObject().getLiteral() )
							          );
				} else if ( (conclusionOntology == null) && 
						    ( dataAxiom.getProperty().toString().equals("<http://www.w3.org/2007/OWL/testOntology#rdfXmlConclusionOntology>") ||
						      dataAxiom.getProperty().toString().equals("<http://www.w3.org/2007/OWL/testOntology#fsConclusionOntology>") ) ) {
					//							System.out.println("ConclusionOntology:");
					//							System.out.println(dataAxiom.getObject().getLiteral());
					conclusionOntology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(
					                       (OWLOntologyDocumentSource)new StringDocumentSource(dataAxiom.getObject().getLiteral())
					                     );
				} else if ( (nonConclusionOntology == null) && 
						    ( dataAxiom.getProperty().toString().equals("<http://www.w3.org/2007/OWL/testOntology#rdfXmlNonConclusionOntology>") ||
						      dataAxiom.getProperty().toString().equals("<http://www.w3.org/2007/OWL/testOntology#fsNonConclusionOntology>") ) ) {
					//							System.out.println("ConclusionOntology:");
					//							System.out.println(dataAxiom.getObject().getLiteral());
					nonConclusionOntology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(
					                         (OWLOntologyDocumentSource)new StringDocumentSource(dataAxiom.getObject().getLiteral())
					                        );
				}
			}
		}
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
	
	protected void reportResult(TestResult result, String testcase, TestType testtype) {
		try {
			if ( result == TestResult.FAIL ) {
				outputbuffer.write("FAILED " + testcase + " (" + testtype + ")\n");
			} else if ( result == TestResult.PASS ) {
				outputbuffer.write("   PASSED " + testcase + " (" + testtype + ")\n");
			} else if ( result == TestResult.INCOMPLETE ) {
				outputbuffer.write("INCOMPLETE " + testcase + " (" + testtype + ")\n");
			}
		} catch (IOException e) {
			System.err.println("Problem writing test case output: " + e.toString());
		}
	}
	
}	

