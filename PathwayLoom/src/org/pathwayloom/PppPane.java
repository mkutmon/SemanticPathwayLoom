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

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.pathvisio.core.Engine;
import org.pathvisio.core.model.DataNodeType;
import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.view.VPathway;
import org.pathvisio.desktop.PvDesktop;
import org.pathvisio.gui.view.VPathwaySwing;


/**
 * A side panel to which Putative Pathway Parts can be added.
 */
public class PppPane extends JPanel
{
	static final String TITLE = "Loom";
	PvDesktop desktop;
	JPanel panel;
	//static Engine engine = desktop.;

	private static class CopyAction extends AbstractAction
	{
		private final VPathway vPwy;
		CopyAction (VPathway vPwy)
		{
			this.vPwy = vPwy;
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, 
					Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			putValue(NAME, "Copy");
		}

		public void actionPerformed(ActionEvent arg0) 
		{
			vPwy.copyToClipboard();
		}
	}

	/**
	 * Add a new Pathway part to the panel, with the given description displayed above it.
	 */
	public void addPart(String desc, Pathway part)
	{
		panel.removeAll();

		panel.add (new JLabel(desc));
		JScrollPane scroller =  
			new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroller.setMinimumSize(new Dimension(150, 150));
		VPathwaySwing wrapper = new VPathwaySwing(scroller);
		VPathway vPwy = wrapper.createVPathway();
		vPwy.setEditMode(false);
		vPwy.fromModel(part);
		vPwy.setPctZoom(66.7);
		vPwy.addVPathwayListener(desktop.getVisualizationManager());
		CopyAction a = new CopyAction(vPwy);
		wrapper.registerKeyboardAction((KeyStroke)a.getValue(Action.ACCELERATOR_KEY), a);
		scroller.add(wrapper);

		panel.add (scroller);


		List<Xref> xrefs = part.getDataNodeXrefs();
		//System.out.println(part.getElementById(part.getGraphIds().iterator().next()).get.getTextLabel());
		final DefaultTableModel model = new DefaultTableModel() {

			@Override
			public boolean isCellEditable(int row, int column) {
				//all cells false
				return false;
			}
		};

		final JTable table = new JTable(model); 
		table.setColumnSelectionAllowed(true);
		table.setRowSelectionAllowed(true);
		table.setColumnSelectionAllowed(false);
		table.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e){	    	 
				int selectedRow = table.getSelectedRow();
				String selectedLabel = (String) model.getValueAt(selectedRow, 0);
				String selectedDataSource = (String) model.getValueAt(selectedRow, 1);
				String selectedIdentifier = (String) model.getValueAt(selectedRow, 2);
				String selectedDataNodeType = (String) model.getValueAt(selectedRow, 3);
				String selectedWebsite = (String) model.getValueAt(selectedRow, 4);
				String parentGraphId = (String) model.getValueAt(selectedRow, 5);
				Engine engine = desktop.getSwingEngine().getEngine(); 
				String inputEntity = engine.getActivePathway().getElementById(parentGraphId).getTextLabel();
				

				
				if (SwingUtilities.isRightMouseButton(e)){
					

					final JComponent[] inputs = new JComponent[] {
							new JLabel(inputEntity),
							new JLabel("Label: "+selectedLabel),
							new JLabel("Datasource: "+selectedDataSource),
							new JLabel("Identifier: "+selectedIdentifier),
							new JLabel("Authority: "+selectedIdentifier),
					};
					JOptionPane.showMessageDialog(null,inputs,"TITLE",JOptionPane.INFORMATION_MESSAGE);
				}

				if (e.getClickCount() == 2){

					PathwayElement pel = PathwayElement.createPathwayElement(ObjectType.DATANODE);
					// pel.setDataNodeType(DataNodeType.GENEPRODUCT);
					System.out.println(selectedDataNodeType);
					if (selectedDataNodeType.equals("Unkown")){
						pel.setDataNodeType(DataNodeType.UNKOWN);
					}
					if (selectedDataNodeType.equals("Metabolite")){
						pel.setDataNodeType(DataNodeType.METABOLITE);
					}
					if (selectedDataNodeType.equals("GeneProduct")){
						pel.setDataNodeType(DataNodeType.GENEPRODUCT);
					}
					if (selectedDataSource.equals("HMDB")){
						pel.setDataSource(BioDataSource.HMDB);
					}
					if (selectedDataSource.contains("Uniprot")){
						pel.setDataSource(BioDataSource.UNIPROT);
					}
					pel.setMCenterX(engine.getActivePathway().getElementById(parentGraphId).getMCenterX()+165);
					pel.setMCenterY(engine.getActivePathway().getElementById(parentGraphId).getMCenterY());
					pel.setMHeight(20);
					pel.setMWidth(80);
					pel.setTextLabel(selectedLabel.replace("@en", ""));
					pel.setGeneID(selectedIdentifier);
					pel.setGraphId(engine.getActivePathway().getUniqueGraphId());
					//  pel.setDataSource(v)
					engine.getActivePathway().add(pel);

					PathwayElement connectElement = PathwayElement.createPathwayElement(ObjectType.LINE);
					connectElement.setMStartX(pel.getMCenterX());
					connectElement.setMStartY(pel.getMCenterY());
					connectElement.setMEndX(engine.getActivePathway().getElementById(parentGraphId).getMCenterX());
					connectElement.setMEndY(engine.getActivePathway().getElementById(parentGraphId).getMCenterY());
					connectElement.setStartGraphRef(parentGraphId);
					connectElement.setEndGraphRef(pel.getGraphId());
					engine.getActivePathway().add(connectElement);


				}
			}
		} );
		// Create a couple of columns 
		model.addColumn("Label");
		model.addColumn("DataSource"); 
		model.addColumn("Identifier"); 
		model.addColumn("Type");
		model.addColumn("Website");
		model.addColumn("graphid");

		// Append a row 
		Set<String> graphIds = part.getGraphIds();
		Iterator<String> graphIdIterator = graphIds.iterator();
		while (graphIdIterator.hasNext()){
			String graphId = graphIdIterator.next();
			PathwayElement pwElement = part.getElementById(graphId);
			String textLabel = pwElement.getTextLabel();
			String datasource = pwElement.getXref().getDataSource().getFullName();
			String datanodetype = pwElement.getDataNodeType();
			String identifier = pwElement.getXref().getId();
			String website = pwElement.getXref().getUrl();
			String parentGraphId = pwElement.findComment("ParentGraphId");
			System.out.println(pwElement.findComment("ParentGraphId"));
			model.addRow(new Object[]{textLabel, datasource, identifier, datanodetype, website, parentGraphId});
		}



		JScrollPane scrollPane = new JScrollPane(table);
		//table.setFillsViewportHeight(true);

		panel.add(scrollPane);

		JTabbedPane pane = desktop.getSwingEngine().getApplicationPanel().getSideBarTabbedPane();
		int index = pane.indexOfTab(TITLE);
		if (index > 0)
		{
			pane.setSelectedIndex (index);
		}
		validate();
	}

	/**
	 * Create a new Ppp Pane with Help button. Parts can be added later.
	 */
	public PppPane (final PvDesktop desktop)
	{
		this.desktop = desktop;

		setLayout (new BorderLayout());

		panel = new JPanel ();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		add (new JScrollPane (panel), BorderLayout.CENTER);

		// help button
		JButton help = new JButton("Help");
		JButton clear = new JButton("Clear");
		//JButton drawDataNode = new JButton("Draw New Datanode");
		panel.add (help);
		panel.add(clear);

		JTextPane detailpane = new JTextPane();
		detailpane.setContentType("text/html");
		detailpane.setEditable(false);
		try {
			detailpane.setPage("file:///tmp/test.html");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		panel.add(detailpane);
		//panel.add(drawDataNode);

		clear.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				new Thread (new Runnable() 
				{		
					public void run() 
					{
						try
						{

							//panel;
							//panel.revalidate();
						}
						catch (Exception ex)
						{
							javax.swing.SwingUtilities.invokeLater(new Runnable() 
							{		
								public void run() 
								{
									JOptionPane.showMessageDialog (desktop.getFrame(), 
											"Could not launch browser\nSee error log for details.", 
											"Error", JOptionPane.ERROR_MESSAGE);
								}
							});
						}
					}
				}).start();
			}
		});	

		help.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				new Thread (new Runnable() 
				{		
					public void run() 
					{
						try
						{
							Desktop.getDesktop().browse(new URI("http://www.pathvisio.org/Ppp"));
						}
						catch (Exception ex)
						{
							javax.swing.SwingUtilities.invokeLater(new Runnable() 
							{		
								public void run() 
								{
									JOptionPane.showMessageDialog (desktop.getFrame(), 
											"Could not launch browser\nSee error log for details.", 
											"Error", JOptionPane.ERROR_MESSAGE);
								}
							});
						}
					}
				}).start();
			}
		});

	}	
}

