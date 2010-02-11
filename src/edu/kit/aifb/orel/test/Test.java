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
public class Test {
	/**
	 * Class to load a testOnotology and separate the premise and conclusion ontologies. 
	 * @author Anees
	 */

	private OWLOntologyManager manager;
	private  IRI physicalURI;
	private OWLOntology ontology;
	public OWLOntology premiseOntology;
	public OWLOntology conclusionOntology;
	String file;
	
	/**
	 * @param file (String) containing the ontology test 
	 */
	public Test(String file) throws OWLOntologyCreationException, SQLException, IOException {
		this.file = file;
		loadOntology();
		
	}
	private void loadOntology() throws OWLOntologyCreationException {
		manager = OWLManager.createOWLOntologyManager();
		physicalURI= IRI.create((new File(System.getProperty("user.dir")+"/testCases/"+this.file)).toURI());
		//		physicalURI= URI.create("http://owl.semanticweb.org/exports/proposed/RL-RDF-rules-tests.rdf");

		ontology = manager.loadOntologyFromOntologyDocument(physicalURI);
	}
	public OWLOntology getPremiseOntology() {
		return premiseOntology;
	}

	

	public OWLOntology getConclusionOntology() {
		return conclusionOntology;
	}

	public void test(BasicKBManager kbmanager) throws Exception{
		BufferedWriter result = new BufferedWriter(new FileWriter("result.txt"));
	
	    
		Set<OWLClassAssertionAxiom> axiomset=ontology.getAxioms(AxiomType.CLASS_ASSERTION);
		Set<OWLDataPropertyAssertionAxiom> dataset=ontology.getAxioms(AxiomType.DATA_PROPERTY_ASSERTION);
		Iterator<OWLDataPropertyAssertionAxiom> dataIterator;
		Iterator<OWLClassAssertionAxiom> classIterator=axiomset.iterator();
		OWLClassAssertionAxiom classAssert;
		
		
		while(classIterator.hasNext()){
		
			classAssert=classIterator.next();
//			result.write(classAssert.getClassExpression().toString());
//			result.newLine();
//			Positive Entailment Test
			if(classAssert.getClassExpression().toString().equals("<http://www.w3.org/2007/OWL/testOntology#PositiveEntailmentTest>")){
				result.write("Positive Entailment Test:");
				result.write(classAssert.getIndividual().toString());
//				result.write("classAssert"+classAssert.getClassExpression().getClass().getName());
				result.newLine();
				dataIterator=dataset.iterator();
				setPremiseConclusionOntologies(dataset, classAssert);
//				Testing
				if(kbmanager.loadOntology(premiseOntology)){
					InferenceResult infResult=kbmanager.checkEntailment(conclusionOntology);
					if(infResult==InferenceResult.YES){
						System.out.println("Passed");
						result.write("Passed");
						result.newLine();
					}
					else if(infResult==InferenceResult.NO){
						System.out.println("Fail");
						result.write("Fail");
						result.newLine();
					}
					else if(infResult==InferenceResult.DONTKNOW){
						result.write("Incomplete");
						result.write("Incomplete");
						result.newLine();

					}
					kbmanager.drop();
					kbmanager.initialize();
				}
				else{
					InferenceResult infResult=kbmanager.checkEntailment(conclusionOntology);
					if(infResult==InferenceResult.YES){
						System.out.println("Passed");
						result.write("Passed");
						result.newLine();
					}
					else if(infResult==InferenceResult.NO){
						System.out.println("Incomplete");
						result.write("Incomplete");
						result.newLine();
					}
					else if(infResult==InferenceResult.DONTKNOW){
						result.write("Incomplete");
						result.write("Incomplete");
						result.newLine();

					}
					kbmanager.drop();
					kbmanager.initialize();
				}
			}
//			Negative Entailment Test
			else if(classAssert.getClassExpression().toString().equals("<http://www.w3.org/2007/OWL/testOntology#NegativeEntailmentTest>")){
				result.write("Negative Entailment Test:");
				result.write(classAssert.getIndividual().toString());
//				result.write("classAssert"+classAssert.getClassExpression().getClass().getName());
				result.newLine();
				setPremiseConclusionOntologies(dataset, classAssert);
//				Testing
				if(kbmanager.loadOntology(premiseOntology)){
					InferenceResult infResult=kbmanager.checkEntailment(conclusionOntology);
					if(infResult==InferenceResult.YES){
						System.out.println("Fail");
						result.write("Fail");
						result.newLine();
					}
					else if(infResult==InferenceResult.NO){
						System.out.println("Passed");
						result.write("Passed");
						result.newLine();
					}
					else if(infResult==InferenceResult.DONTKNOW){
						result.write("Incomplete");
						result.write("Incomplete");
						result.newLine();

					}
					kbmanager.drop();
					kbmanager.initialize();
				}
				else{
					InferenceResult infResult=kbmanager.checkEntailment(conclusionOntology);
					if(infResult==InferenceResult.YES){
						System.out.println("Fail");
						result.write("Fail");
						result.newLine();
					}
					else if(infResult==InferenceResult.NO){
						System.out.println("Incomplete");
						result.write("Incomplete");
						result.newLine();
					}
					else if(infResult==InferenceResult.DONTKNOW){
						result.write("Incomplete");
						result.write("Incomplete");
						result.newLine();

					}
					kbmanager.drop();
					kbmanager.initialize();
				}
			}
//			Consistency Test
				else if(classAssert.getClassExpression().toString().equals("<http://www.w3.org/2007/OWL/testOntology#ConsistencyTest>")){
					
					result.write("Consistency Test:");
					result.write(classAssert.getIndividual().toString());
//					result.write("classAssert"+classAssert.getClassExpression().getClass().getName());
					result.newLine();
					setPremiseOntology(dataset, classAssert);
					if(classAssert.getIndividual().toString().equals("<http://owl.semanticweb.org/id/TestCase-3AWebOnt-2DequivalentClass-2D001>")){
						System.out.println("Here is the problem");
					}
//					Testing
					if(kbmanager.loadOntology(premiseOntology)){
						InferenceResult infResult=kbmanager.checkConsistency();
						if(infResult==InferenceResult.YES){
							System.out.println("Passed");
							result.write("Passed");
							result.newLine();
						}
						
						else if(infResult==InferenceResult.NO){
							System.out.println("Fail");
							result.write("Fail");
							result.newLine();
						}
						else if(infResult==InferenceResult.DONTKNOW){
							result.write("Incomplete");
							result.write("Incomplete");
							result.newLine();

						}
						kbmanager.drop();
						kbmanager.initialize();
					}
					else{
						InferenceResult infResult=kbmanager.checkConsistency();
						if(infResult==InferenceResult.YES){
							System.out.println("Incomplete");
							result.write("Incomplete");
							result.newLine();
						}
						
						else if(infResult==InferenceResult.NO){
							System.out.println("Fail");
							result.write("Fail");
							result.newLine();
						}
						else if(infResult==InferenceResult.DONTKNOW){
							result.write("Incomplete");
							result.write("Incomplete");
							result.newLine();
						}
						kbmanager.drop();
						kbmanager.initialize();
					}
			
				}
//			Inconsistency Test
				else if(classAssert.getClassExpression().toString().equals("<http://www.w3.org/2007/OWL/testOntology#InconsistencyTes>")){
					result.write("InConsistency Test:");
					result.write(classAssert.getIndividual().toString());
					if(classAssert.getIndividual().toString().equals("<http://owl.semanticweb.org/id/TestCase-3AWebOnt-2DequivalentClass-2D001>")){
						System.out.println("Check it here");
					}
//					result.write("classAssert"+classAssert.getClassExpression().getClass().getName());
					result.newLine();
					setPremiseOntology(dataset, classAssert);
//					Testing
					if(kbmanager.loadOntology(premiseOntology)){
						InferenceResult infResult=kbmanager.checkConsistency();
						if(infResult==InferenceResult.YES){
							System.out.println("Fail");
							result.write("Fail");
							result.newLine();
						}
						else if(infResult==InferenceResult.NO){
							System.out.println("Passed");
							result.write("Passed");
							result.newLine();
						}
						else if(infResult==InferenceResult.DONTKNOW){
							result.write("Incomplete");
							result.write("Incomplete");
							result.newLine();

						}
						kbmanager.drop();
						kbmanager.initialize();
					}
					else{
						InferenceResult infResult=kbmanager.checkConsistency();
						if(infResult==InferenceResult.YES){
							System.out.println("Incomplete");
							result.write("Incomplete");
							result.newLine();
						}
						else if(infResult==InferenceResult.NO){
							System.out.println("Passed");
							result.write("Passed");
							result.newLine();
						}
						else if(infResult==InferenceResult.DONTKNOW){
							result.write("Incomplete");
							result.write("Incomplete");
							result.newLine();

						}
						kbmanager.drop();
						kbmanager.initialize();
					}
			
				}
				else{
					result.write("Unknown Test Case:");
					result.write(classAssert.getIndividual().toString());
					result.newLine();
					
				}
			result.newLine();
			result.flush();
			
		}
		result.close();
	}
	
		// TODO Auto-generated method stub
		
	
	private void setPremiseOntology(Set<OWLDataPropertyAssertionAxiom> dataset,
			OWLClassAssertionAxiom classAssert) throws OWLOntologyCreationException {
		OWLDataPropertyAssertionAxiom dataAxiom;
		// 	OWLDataPropertyAssertionAxiom dataAxiom;
		Iterator<OWLDataPropertyAssertionAxiom> dataIterator = dataset.iterator();
		while(dataIterator.hasNext()){
			dataAxiom=dataIterator.next();
			if(dataAxiom.getSubject().equals(classAssert.getIndividual())) {
				if(dataAxiom.getProperty().toString().equals("<http://www.w3.org/2007/OWL/testOntology#rdfXmlPremiseOntology>")){
					//							System.out.println("PremiseOntology:");
					//							System.out.println(dataAxiom.getObject().getLiteral());
					String ontStr=dataAxiom.getObject().getLiteral();
					OWLOntologyManager man=OWLManager.createOWLOntologyManager();
					premiseOntology =man.loadOntologyFromOntologyDocument((OWLOntologyDocumentSource)new StringDocumentSource(ontStr));
					if(classAssert.getIndividual().toString().equals("<http://owl.semanticweb.org/id/TestCase-3AWebOnt-2DequivalentClass-2D001>")){
						System.out.println(classAssert);
						System.out.println(ontStr);

					}
				}
			
			}

		}
		
		
	}
	void setPremiseConclusionOntologies(Set<OWLDataPropertyAssertionAxiom> dataset, OWLClassAssertionAxiom classAssert) throws OWLOntologyCreationException{
		OWLDataPropertyAssertionAxiom dataAxiom;
		Iterator<OWLDataPropertyAssertionAxiom> dataIterator = dataset.iterator();
		while(dataIterator.hasNext()){
			dataAxiom=dataIterator.next();
			if(dataAxiom.getSubject().equals(classAssert.getIndividual())) {
				if(dataAxiom.getProperty().toString().equals("<http://www.w3.org/2007/OWL/testOntology#rdfXmlPremiseOntology>")){
					//							System.out.println("PremiseOntology:");
					//							System.out.println(dataAxiom.getObject().getLiteral());
					String ontStr=dataAxiom.getObject().getLiteral();
					OWLOntologyManager man=OWLManager.createOWLOntologyManager();
					premiseOntology =man.loadOntologyFromOntologyDocument((OWLOntologyDocumentSource)new StringDocumentSource(ontStr));
				}
				else if(dataAxiom.getProperty().toString().equals("<http://www.w3.org/2007/OWL/testOntology#rdfXmlConclusionOntology>")){
					//							System.out.println("ConclusionOntology:");
					//							System.out.println(dataAxiom.getObject().getLiteral());
					String ontStr=dataAxiom.getObject().getLiteral();
					OWLOntologyManager man=OWLManager.createOWLOntologyManager();
					conclusionOntology =man.loadOntologyFromOntologyDocument((OWLOntologyDocumentSource)new StringDocumentSource(ontStr));
				}
			}

		}
		
	}
	
}	









