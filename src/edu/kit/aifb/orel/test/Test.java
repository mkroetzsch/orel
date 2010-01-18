package edu.kit.aifb.orel.test;


import org.semanticweb.owlapi.apibinding.*;
import org.semanticweb.owlapi.io.OWLOntologyInputSource;
import org.semanticweb.owlapi.io.StringInputSource;
import org.semanticweb.owlapi.model.*;

import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.io.*;
public class Test {

	/**
	 * @param args
	 */

	private OWLOntologyManager manager;
	private  URI physicalURI;
	private OWLOntology ontology;
	public void loadOntology(String file) throws OWLOntologyCreationException{
		manager = OWLManager.createOWLOntologyManager();
		physicalURI= (new File(System.getProperty("user.dir")+"/"+file)).toURI();
		//		physicalURI= URI.create("http://owl.semanticweb.org/exports/proposed/RL-RDF-rules-tests.rdf");

		ontology = manager.loadOntologyFromPhysicalURI(physicalURI);
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
				System.out.println("--------------------------------------------------");
				System.out.println("--------------------------------------------------");
				System.out.println("Positive Entailment Test");
				System.out.println("TestName::"+classAssert.getIndividual());
				dataIterator=dataset.iterator();
				while(dataIterator.hasNext()){
					dataAxiom=dataIterator.next();
					if(dataAxiom.getSubject().equals(classAssert.getIndividual())) {
						System.out.println(dataAxiom.getProperty());
						System.out.println("Huura");
						if(dataAxiom.getProperty().toString().equals("<http://www.w3.org/2007/OWL/testOntology#rdfXmlPremiseOntology>")){
							System.out.println("PremiseOntology:");
							System.out.println(dataAxiom.getObject().getLiteral());
							String ontStr=dataAxiom.getObject().getLiteral();
							OWLOntologyManager man=OWLManager.createOWLOntologyManager();
							OWLOntology o =man.loadOntology((OWLOntologyInputSource)new  StringInputSource(ontStr));
							System.out.println("NOW PARSING THE NEW ONTOLOGY");
							System.out.println("Successfully Loaded");
							Set<OWLAxiom> x=o.getAxioms();
							Iterator<OWLAxiom> i=x.iterator();

							while(i.hasNext()){
								System.out.println(i.next());
							}

						}
						else if(dataAxiom.getProperty().toString().equals("<http://www.w3.org/2007/OWL/testOntology#rdfXmlConclusionOntology>")){
							System.out.println("ConclusionOntology:");
							System.out.println(dataAxiom.getObject().getLiteral());
						}
					}

				}

			}
		}
	}		
}	









