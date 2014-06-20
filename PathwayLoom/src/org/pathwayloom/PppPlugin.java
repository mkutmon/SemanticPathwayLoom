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

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingWorker;

import org.bridgedb.DataSource;
import org.bridgedb.Xref;
import org.pathvisio.core.data.GdbManager;
import org.pathvisio.core.debug.Logger;
import org.pathvisio.core.gpmldiff.PwyDoc;
import org.pathvisio.core.model.ConverterException;
import org.pathvisio.core.model.GpmlFormat;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.util.ProgressKeeper;
import org.pathvisio.core.view.GeneProduct;
import org.pathvisio.core.view.VPathwayElement;
import org.pathvisio.core.view.VPathway;
import org.pathvisio.desktop.PvDesktop;
import org.pathvisio.desktop.plugin.Plugin;
import org.pathvisio.gui.ProgressDialog;
import org.pathvisio.gui.PathwayElementMenuListener.PathwayElementMenuHook;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Putative Parts plug-in - 
 * shows a sidebar where other plug-ins can add suggestions.
 * 
 * Also hooks into the right-click menu
 */
public class PppPlugin implements Plugin, PathwayElementMenuHook
{
	public static final double DATANODE_MWIDTH = 60;
	public static final double DATANODE_MHEIGHT = 20;

	private PvDesktop desktop;
	PppPane pane;
	SuggestionAction sparqlLoomAllAction;
	SuggestionAction sparqlLoomGPAction;
	SuggestionAction sparqlLoomMBAction;

	ArrayList<SuggestionAction> cwSAList;



	/**
	 * return the existing PppPane
	 */
	public PppPane getPane()
	{
		return pane;
	}

	/**
	 * Initialize plug-in, is called by plugin manager.
	 */
	public void init(PvDesktop desktop) 
	{

		// create new PppPane and add it to the side bar.
		pane = new PppPane(desktop);
		JTabbedPane sidebarTabbedPane = desktop.getSideBarTabbedPane();
		sidebarTabbedPane.add(PppPane.TITLE, pane);
		// register our pathwayElementMenu hook.
		desktop.addPathwayElementMenuHook(this);
		this.desktop = desktop;
		GdbManager gdbManager = desktop.getSwingEngine().getGdbManager();
		sparqlLoomAllAction = new SuggestionAction(this, "Get all known relations", new SparqlLoomAllPlugin(gdbManager));
		sparqlLoomGPAction = new SuggestionAction(this, "Get related gene products", new SparqlLoomGPPlugin(gdbManager));
		sparqlLoomMBAction = new SuggestionAction(this, "Get related metabolites", new SparqlLoomMBPlugin(gdbManager));

	}

	public void done() {}

	/**
	 * Action to be added to right-click menu.
	 * This action is recycled, i.e. it's instantiated only once but
	 * added each time to the new popup Menu.
	 */




	private class SuggestionAction extends AbstractAction
	{
		private final PppPlugin parent;
		private final String name;

		GeneProduct elt;

		/**
		 * set the element that will be used as input for the suggestion.
		 * Call this before adding to the menu. 
		 */
		void setElement (GeneProduct anElt)
		{
			elt = anElt;
			setEnabled (suggestion.canSuggest(elt.getPathwayElement()));
		}

		private final Suggestion suggestion;

		/** called when plug-in is initialized */
		SuggestionAction(PppPlugin aParent, String name, Suggestion suggestion)
		{
			parent = aParent;
			this.suggestion = suggestion;
			this.name = name;
			putValue(NAME, name );
		}

		/** called when user clicks on the menu item */



		public void actionPerformed(ActionEvent e) 
		{
			final PppPane pane = parent.getPane();
			//pane.removeAll();
			//pane.validate();
			final ProgressKeeper pk = new ProgressKeeper();
			final ProgressDialog pd = new ProgressDialog(desktop.getFrame(), "Querying " +  name, pk, true, true);
			pk.setTaskName("Running query");

			SwingWorker<Pathway, Void> worker = new SwingWorker<Pathway, Void>()
			{

				@Override
				protected Pathway doInBackground() throws Exception 
				{
					Pathway result = suggestion.doSuggestion(elt.getPathwayElement());
					return result;
				}

				@Override
				protected void done()
				{
					if (pk.isCancelled()) return; // don't add if user pressed cancel.
					try {
						pane.addPart("Pathway Loom: " + name, get());
					} 
					catch (InterruptedException e) 
					{
						Logger.log.error("Operation interrupted", e);
						JOptionPane.showMessageDialog(
								pane, "Operation interrupted",
								"Error", JOptionPane.ERROR_MESSAGE
						);
					} 
					catch (ExecutionException e) 
					{
						// exception generated by the suggestion implementation itself
						Throwable cause = e.getCause();
						cause.printStackTrace();
						Logger.log.error("Unable to get suggestions", cause);
						JOptionPane.showMessageDialog(
								pane, "Unable to get suggestions: " + cause.getMessage(),
								"Error", JOptionPane.ERROR_MESSAGE
						);
					}
					pk.finished();
				}
			};
			worker.execute();
			pd.setVisible(true);
		}
	}

	/**
	 * callback, is called when user clicked with RMB on a pathway element.
	 * @throws IOException 
	 * @throws ConverterException 
	 */

		


	public void pathwayElementMenuHook(final VPathwayElement e, JPopupMenu menu) 
	{
        
		if (e instanceof GeneProduct)
		{
			JMenu submenu = new JMenu("Pathway Loom");
			JMenuItem titleMenu = submenu.add("Pathway Loom BETA");
			titleMenu.setEnabled(false);
			titleMenu.setFont(new Font("sansserif", Font.BOLD, 16));
			titleMenu.setBackground(Color.white);
			titleMenu.setForeground(Color.gray);
			
			JMenuItem binaryRelationsMeny = submenu.add("Binary relations");
			binaryRelationsMeny.setEnabled(false);
			binaryRelationsMeny.setBackground(Color.orange);
			binaryRelationsMeny.setForeground(Color.yellow);
			//submenu.add("Get interaction suggestions");
			
			
			sparqlLoomAllAction.setElement((GeneProduct) e);
			sparqlLoomGPAction.setElement((GeneProduct) e);
			sparqlLoomMBAction.setElement((GeneProduct) e);
			
			submenu.add(sparqlLoomAllAction);
			submenu.add(sparqlLoomGPAction);
			submenu.add(sparqlLoomMBAction);
			
			JMenuItem NanoPublicationMenu = submenu.add("Nanopublications");
			NanoPublicationMenu.setEnabled(false);
            NanoPublicationMenu.setBackground(Color.orange);
            NanoPublicationMenu.setForeground(Color.yellow);
			
			JMenuItem localDataMenu = submenu.add("Open PHACTS");
			localDataMenu.setEnabled(false);
			localDataMenu.setBackground(Color.orange);
			localDataMenu.setForeground(Color.yellow);
			//submenu.add(openPhactsCompoundPharmaAPI);
			//submenu.add("Get target compounds");
			menu.add(submenu);


		}

	}


}

