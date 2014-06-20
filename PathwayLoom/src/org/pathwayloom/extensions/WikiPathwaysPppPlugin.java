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
package org.pathwayloom.extensions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bridgedb.bio.BioDataSource;
import org.pathvisio.core.data.GdbManager;
import org.pathvisio.core.model.ConverterException;
import org.pathvisio.core.model.DataNodeType;
import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.pathwayloom.PathwayBuilder;
import org.pathwayloom.PppPlugin;
import org.pathwayloom.SuggestionAdapter;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

/**
 * Generates Putative Pathway Parts based on a 
 * HMDB metabolic network parsed and stored in MySQL by Andra.
 */
public class WikiPathwaysPppPlugin extends SuggestionAdapter 
{
	final GdbManager gdbManager;

	WikiPathwaysPppPlugin (GdbManager gdbManager)
	{
		this.gdbManager = gdbManager;
	}

	@Override public Pathway doSuggestion(PathwayElement input) throws SuggestionException 
	{
		/*	try {
            // The newInstance() call is a work around for some
            // broken Java implementations
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (Exception ex) {
            // handle the error
        }*/

		//System.out.println(wikiPathwaysInteractionsLst.getLength());
		PathwayElement pelt = PathwayElement.createPathwayElement(ObjectType.DATANODE);
		pelt.setMWidth (PppPlugin.DATANODE_MWIDTH);
		pelt.setMHeight (PppPlugin.DATANODE_MHEIGHT);
		pelt.setTextLabel(input.getTextLabel());
		pelt.setDataSource(input.getDataSource()); //TODO omvormen naar RDF
		pelt.setGeneID(input.getGeneID());
		pelt.setCopyright("Copyright notice");
		pelt.setDataNodeType(input.getDataNodeType());
		List<PathwayElement> spokes = new ArrayList<PathwayElement>();
		String getSourceDetailsSparql = "prefix dcterms:  <http://purl.org/dc/terms/>" +
		"prefix gpml:    <http://vocabularies.wikipathways.org/gpml#>" +
		"SELECT DISTINCT * WHERE {" + 
		"   ?datanode1 dcterms:identifier \""+input.getGeneID()+"\"^^<http://www.w3.org/2001/XMLSchema#string> ." + 
		"   ?datanode1 gpml:graphid ?dn1GraphId ." + 
		"   ?datanode1 a gpml:DataNode ." + 
		"   ?datanode1 dcterms:isPartOf ?pathway ." + 
		"   ?line gpml:graphref ?dn1GraphId ." + 
		"   ?line a gpml:Line ." + 
		"   ?line gpml:graphid ?lineGraphId ." + 
		"   ?line dcterms:isPartOf ?pathway ." + 
		"}";
		Query query = QueryFactory.create(getSourceDetailsSparql);
		QueryExecution queryExecution = QueryExecutionFactory.sparqlService("http://sparql.wikipathways.org/", query);
		ResultSet resultSet = queryExecution.execSelect();
		while (resultSet.hasNext()) {
			QuerySolution solution = resultSet.next();
			String lineGraphId = solution.get("lineGraphId").toString();
			String pathway = solution.get("pathway").toString();
			String line = solution.get("line").toString();
			String dn1GraphId = solution.get("dn1GraphId").toString();
			String datanode1 = solution.get("datanode1").toString();
			String getTargetDetailsSparql = "prefix dcterms:  <http://purl.org/dc/terms/> " +
			"prefix rdfs:    <http://www.w3.org/2000/01/rdf-schema#>" +
			"prefix gpml:    <http://vocabularies.wikipathways.org/gpml#> " +
			"SELECT DISTINCT * WHERE { " +
			"?datanode2 dcterms:identifier ?dn2Identifier . " +
			"?datanode2 a gpml:DataNode . " +
			"?datanode2 rdfs:label ?dn2Label ." +
			"?datanode2 dcterms:isPartOf <"+pathway+"> . " +
			"?datanode2 gpml:graphid ?dn2GraphId . " +
			"<"+line+"> gpml:graphref ?dn2GraphId . " +
			"FILTER (?datanode2 != <"+datanode1+">) " +
			"}";
			System.out.println(getTargetDetailsSparql);
			Query query2 = QueryFactory.create(getTargetDetailsSparql);
			QueryExecution queryExecution2 = QueryExecutionFactory.sparqlService("http://sparql.wikipathways.org", query2);
			ResultSet resultSet2 = queryExecution2.execSelect();
			System.out.println(resultSet2.getResultVars());
			System.out.println(resultSet2.getRowNumber());
			while (resultSet2.hasNext()) {
				QuerySolution solution2 = resultSet2.next();
				System.out.println(solution2.get("datanode2").toString());
				PathwayElement pchildElt = PathwayElement.createPathwayElement(ObjectType.DATANODE);
				pchildElt.setDataNodeType (DataNodeType.UNKOWN); //TODO fix datasource in SPARQL query
				pchildElt.setTextLabel(solution2.get("dn2Label").toString());
				pchildElt.setGeneID(solution2.get("dn2Identifier").toString());
				pchildElt.setMWidth (PppPlugin.DATANODE_MWIDTH);
				pchildElt.setMHeight (PppPlugin.DATANODE_MHEIGHT);
				spokes.add (pchildElt);
			}

		}
		Pathway result = PathwayBuilder.radialLayout(pelt, spokes);
		return result;

	}

	/**
	 * @param args
	 * @throws ConverterException 
	 */
	public static void main(String[] args) throws IOException, ConverterException, SuggestionException
	{
		WikiPathwaysPppPlugin hmdbPpp = new WikiPathwaysPppPlugin(null);

		PathwayElement test = PathwayElement.createPathwayElement(ObjectType.DATANODE);
		test.setDataNodeType(DataNodeType.METABOLITE);
		test.setTextLabel("P53");
		test.setGeneID("HMDB01586");
		test.setDataSource(BioDataSource.HMDB);

		Pathway p = hmdbPpp.doSuggestion(test);

		File tmp = File.createTempFile("hmdbppp", ".gpml");
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
