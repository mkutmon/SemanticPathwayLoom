// PathVisio,
// a tool for data visualization and analysis using Biological Pathways
// Copyright 2006-2009 BiGCaT Bioinformatics
//
// Licensed under the Apache License, Version 2.0 (the "License"); 
// you may not use this file except in compliance with the License. 
// You may obtain a copy of the License at 
// 
// http://www.apache.org/licenses/LICENSE-2.0 
//  
// Unless required by applicable law or agreed to in writing, software 
// distributed under the License is distributed on an "AS IS" BASIS, 
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
// See the License for the specific language governing permissions and 
// limitations under the License.
//
package org.pathwayloom;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.pathvisio.core.data.GdbManager;
import org.pathvisio.core.model.ConverterException;
import org.pathvisio.core.model.DataNodeType;
import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;

/**
 * Generates Putative Pathway Parts based on a 
 * HMDB metabolic network parsed and stored in MySQL by Andra.
 */
public class SparqlLoomGPPlugin extends SuggestionAdapter 
{
	private GdbManager gdbManager;

	public SparqlLoomGPPlugin (GdbManager gdbManager)
	{
		this.gdbManager = gdbManager;
	}

	@Override public Pathway doSuggestion(PathwayElement input) throws SuggestionException
	{	
		List<PathwayElement> spokes = new ArrayList<PathwayElement>();		
		
		/*String sparqlQueryString = "PREFIX stitch: <http://www4.wiwiss.fu-berlin.de/stitch/resource/stitch/>" +
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"+	
			"SELECT DISTINCT ?o ?oLabel WHERE {?s stitch:interactsWith ?o ." +
			"?s rdfs:label \""+input.getTextLabel()+"\" ." +
					" ?o rdfs:label ?oLabel}";
        */
		String ioUri = "<http://identifiers.org/uniprot/"+input.getGeneID()+">";
		String sparqlQueryString = "PREFIX wp: <http://vocabularies.wikipathways.org/wp#>" +
		     " PREFIX dcterms: <http://purl.org/dc/terms/> " +
		     " PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
		     " SELECT DISTINCT * WHERE { " +
		     ioUri+" dcterms:relation ?target ." +
		     "?target rdfs:label ?targetLabel ." +
		     "?target a wp:GeneProduct" +
		     "}" ;
		
		System.out.println(sparqlQueryString);
		Query query = QueryFactory.create(sparqlQueryString);
		QueryExecution queryExecution = QueryExecutionFactory.sparqlService("http://localhost:8890/sparql", query);
		ResultSet resultSet = queryExecution.execSelect();
		PathwayElement pelt = PathwayElement.createPathwayElement(ObjectType.DATANODE);
		pelt.setMWidth (PppPlugin.DATANODE_MWIDTH);
		pelt.setMHeight (PppPlugin.DATANODE_MHEIGHT);
		pelt.setTextLabel(input.getTextLabel());
		pelt.setDataSource(input.getDataSource());
		pelt.setGeneID(input.getGeneID());
		pelt.setCopyright("OPENDATA");
		pelt.setDataNodeType(input.getDataNodeType());

		while (resultSet.hasNext()) {
			QuerySolution solution = resultSet.next();
			RDFNode object = solution.get("target");
			String[] identifierParts = object.toString().split("/");
			PathwayElement pchildElt = PathwayElement.createPathwayElement(ObjectType.DATANODE);
			pchildElt.setDataNodeType (DataNodeType.UNKOWN);
			pchildElt.setTextLabel(solution.get("targetLabel").toString());
			pchildElt.setDataSource (BioDataSource.UNIPROT);
			pchildElt.setGeneID(identifierParts[identifierParts.length-1]);
			pchildElt.setMWidth (PppPlugin.DATANODE_MWIDTH);
			pchildElt.setMHeight (PppPlugin.DATANODE_MHEIGHT);
			spokes.add (pchildElt);
		}

		Pathway result = PathwayBuilder.radialLayout(pelt, spokes);
		return result;
		

	}

	/**
	 * @param args
	 * @throws ConverterException 
	 */
	public static void main(String[] args) throws SuggestionException, ConverterException, IOException
	{
		SparqlLoomGPPlugin hmdbPpp = new SparqlLoomGPPlugin(null);

		PathwayElement test = PathwayElement.createPathwayElement(ObjectType.DATANODE);
		test.setDataNodeType(DataNodeType.GENEPRODUCT);
		test.setTextLabel("P10275");
		test.setGeneID("P10275");
		test.setDataSource(BioDataSource.UNIPROT);

		Pathway p = hmdbPpp.doSuggestion(test);

		File tmp = File.createTempFile("openPhactsPwLoom", ".gpml");
		p.writeToXml(tmp, true);

		BufferedReader br = new BufferedReader(new FileReader(tmp));
		String line;
		while ((line = br.readLine()) != null)
		{
			System.out.println (line);


		}
	}

	@Override public boolean canSuggest(PathwayElement input) 
	{
		String type = input.getDataNodeType();	
		return true;
	}

}
