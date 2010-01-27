package edu.kit.aifb.orel.test;


import org.semanticweb.owlapi.apibinding.*;
import org.semanticweb.owlapi.io.OWLOntologyInputSource;
import org.semanticweb.owlapi.io.StringInputSource;
import org.semanticweb.owlapi.model.*;

import java.net.URI;
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
	private  URI physicalURI;
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
		convert();
	}
	private void loadOntology() throws OWLOntologyCreationException {
		manager = OWLManager.createOWLOntologyManager();
		physicalURI= (new File(System.getProperty("user.dir")+"/"+this.file)).toURI();
		//		physicalURI= URI.create("http://owl.semanticweb.org/exports/proposed/RL-RDF-rules-tests.rdf");

		ontology = manager.loadOntologyFromPhysicalURI(physicalURI);
	}
	public OWLOntology getPremiseOntology() {
		return premiseOntology;
	}

	

	public OWLOntology getConclusionOntology() {
		return conclusionOntology;
	}

	public void convert() throws SQLException, IOException, OWLOntologyCreationException{
		Set<OWLClassAssertionAxiom> axiomset=ontology.getAxioms(AxiomType.CLASS_ASSERTION);
		Set<OWLDataPropertyAssertionAxiom> dataset=ontology.getAxioms(AxiomType.DATA_PROPERTY_ASSERTION);
		Iterator<OWLDataPropertyAssertionAxiom> dataIterator;
		Iterator<OWLClassAssertionAxiom> classIterator=axiomset.iterator();
		OWLClassAssertionAxiom classAssert;
		OWLDataPropertyAssertionAxiom dataAxiom;
		while(classIterator.hasNext()){
			classAssert=classIterator.next();
			System.out.println(classAssert.getClassExpression().toString());
			if(classAssert.getClassExpression().toString().equals("<http://www.w3.org/2007/OWL/testOntology#PositiveEntailmentTest>")){
				dataIterator=dataset.iterator();
				while(dataIterator.hasNext()){
					dataAxiom=dataIterator.next();
					if(dataAxiom.getSubject().equals(classAssert.getIndividual())) {
						System.out.println(dataAxiom.getProperty());
						if(dataAxiom.getProperty().toString().equals("<http://www.w3.org/2007/OWL/testOntology#rdfXmlPremiseOntology>")){
							System.out.println("PremiseOntology:");
							System.out.println(dataAxiom.getObject().getLiteral());
							String ontStr=dataAxiom.getObject().getLiteral();
							OWLOntologyManager man=OWLManager.createOWLOntologyManager();
							premiseOntology =man.loadOntology((OWLOntologyInputSource)new  StringInputSource(ontStr));
													}
						else if(dataAxiom.getProperty().toString().equals("<http://www.w3.org/2007/OWL/testOntology#rdfXmlConclusionOntology>")){
							System.out.println("ConclusionOntology:");
							System.out.println(dataAxiom.getObject().getLiteral());
							String ontStr=dataAxiom.getObject().getLiteral();
							OWLOntologyManager man=OWLManager.createOWLOntologyManager();
							conclusionOntology =man.loadOntology((OWLOntologyInputSource)new  StringInputSource(ontStr));
						}
					}

				}

			}
		}
	}		
}	









