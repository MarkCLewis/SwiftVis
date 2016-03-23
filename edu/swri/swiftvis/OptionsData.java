package edu.swri.swiftvis;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import edu.swri.swiftvis.plot.DataPlotStyle;
import edu.swri.swiftvis.plot.p3d.Style3D;
import edu.swri.swiftvis.plot.util.FontOptions;
import edu.swri.swiftvis.plot.util.FontUser;
import edu.swri.swiftvis.scheme.SVSchemeUtil;
import edu.swri.swiftvis.util.EditableBoolean;
import edu.swri.swiftvis.util.EditableInt;
import edu.swri.swiftvis.util.EditableString;
import edu.swri.swiftvis.util.ThreadHandler;

public class OptionsData implements Serializable {
	public static OptionsData instance() {
		if(inst==null) {
			inst=setup();
            inst.loadClassPath();
            inst.loadScripts();
            URLClassLoader sysClassLoader=(URLClassLoader)ClassLoader.getSystemClassLoader();
            Class<URLClassLoader> sysClass=URLClassLoader.class;
            for(File path:inst.jarPaths) {
                try {
                    Method method=sysClass.getDeclaredMethod("addURL", new Class[]{URL.class});
                    method.setAccessible(true);
                    method.invoke(sysClassLoader,path.toURI().toURL());
                } catch(InvocationTargetException e) {
                    JOptionPane.showMessageDialog(null,"There was an exception loading the path: "+path);
                    e.printStackTrace();
                } catch(NoSuchMethodException e) {
                    JOptionPane.showMessageDialog(null,"There was an exception loading the path: "+path);
                    e.printStackTrace();
                } catch(IllegalAccessException e) {
                    JOptionPane.showMessageDialog(null,"There was an exception loading the path: "+path);
                    e.printStackTrace();
                } catch(MalformedURLException e) {
                    JOptionPane.showMessageDialog(null,"There was an exception loading the path: "+path);
                    e.printStackTrace();
                }
            }
		}
		return inst;
	}
	
	public JDialog getOptionsDialog(JFrame frame) {
		if(dialog==null) {
			dialog=new JDialog(frame,"SwiftVis Options");
			JTabbedPane tabbedPane=new JTabbedPane();
			JPanel northPanel;
			JTextField classField;
			JButton removeButton;
			final JList sourceList=new JList(sourceStrings.toArray());
			final JList filterList=new JList(filterStrings.toArray());
			final JList plotList=new JList(plotStrings.toArray());
			final JList sinkList=new JList(sinkStrings.toArray());
            final JList plot3DList=new JList(plot3DStrings.toArray());
			
			JPanel sourcePanel=new JPanel(new BorderLayout());
			northPanel=new JPanel(new BorderLayout());
			classField=new JTextField();
			northPanel.add(classField,BorderLayout.CENTER);
			classField.addActionListener(new ActionListener() {
				@Override
                public void actionPerformed(ActionEvent e) {
					addClass(((JTextField)e.getSource()).getText(),sourceClasses,sourceStrings);
					sourceList.setListData(sourceStrings.toArray());
					((JTextField)e.getSource()).setText("");
				}
			} );
			sourcePanel.add(northPanel,BorderLayout.NORTH);
			sourcePanel.add(new JScrollPane(sourceList),BorderLayout.CENTER);
			removeButton=new JButton("Remove");
			removeButton.addActionListener(new ActionListener() {
				@Override
                public void actionPerformed(ActionEvent e) {
					int ind=sourceList.getSelectedIndex();
					if(ind<0) {
						JOptionPane.showMessageDialog(sourceList,"You must select a source to remove.");
					} else {
						sourceStrings.remove(ind);
						sourceClasses.remove(ind);
						sourceList.setListData(sourceStrings.toArray());
					}
				}
			} );
			sourcePanel.add(removeButton,BorderLayout.SOUTH);
			tabbedPane.add("Sources",sourcePanel);
			
			JPanel filterPanel=new JPanel(new BorderLayout());
			northPanel=new JPanel(new BorderLayout());
			classField=new JTextField();
			northPanel.add(classField,BorderLayout.CENTER);
			classField.addActionListener(new ActionListener() {
				@Override
                public void actionPerformed(ActionEvent e) {
					addClass(((JTextField)e.getSource()).getText(),filterClasses,filterStrings);
					filterList.setListData(filterStrings.toArray());
					((JTextField)e.getSource()).setText("");
				}
			} );
			filterPanel.add(northPanel,BorderLayout.NORTH);
			filterPanel.add(new JScrollPane(filterList),BorderLayout.CENTER);
			removeButton=new JButton("Remove");
			removeButton.addActionListener(new ActionListener() {
				@Override
                public void actionPerformed(ActionEvent e) {
					int ind=filterList.getSelectedIndex();
					if(ind<0) {
						JOptionPane.showMessageDialog(filterList,"You must select a filter to remove.");
					} else {
						filterStrings.remove(ind);
						filterClasses.remove(ind);
						filterList.setListData(filterStrings.toArray());
					}
				}
			} );
			filterPanel.add(removeButton,BorderLayout.SOUTH);
			tabbedPane.add("Filters",filterPanel);
			
			JPanel plotPanel=new JPanel(new BorderLayout());
			northPanel=new JPanel(new BorderLayout());
			classField=new JTextField();
			northPanel.add(classField,BorderLayout.CENTER);
			classField.addActionListener(new ActionListener() {
				@Override
                public void actionPerformed(ActionEvent e) {
					addClass(((JTextField)e.getSource()).getText(),plotClasses,plotStrings);
					plotList.setListData(plotStrings.toArray());
					((JTextField)e.getSource()).setText("");
				}
			} );
			plotPanel.add(northPanel,BorderLayout.NORTH);
			plotPanel.add(new JScrollPane(plotList),BorderLayout.CENTER);
			removeButton=new JButton("Remove");
			removeButton.addActionListener(new ActionListener() {
				@Override
                public void actionPerformed(ActionEvent e) {
					int ind=plotList.getSelectedIndex();
					if(ind<0) {
						JOptionPane.showMessageDialog(plotList,"You must select a plot to remove.");
					} else {
						plotStrings.remove(ind);
						plotClasses.remove(ind);
						plotList.setListData(plotStrings.toArray());
					}
				}
			} );
			plotPanel.add(removeButton,BorderLayout.SOUTH);
			tabbedPane.add("Plot Styles",plotPanel);
			
            JPanel plot3DPanel=new JPanel(new BorderLayout());
            northPanel=new JPanel(new BorderLayout());
            classField=new JTextField();
            northPanel.add(classField,BorderLayout.CENTER);
            classField.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    addClass(((JTextField)e.getSource()).getText(),plot3DClasses,plot3DStrings);
                    plot3DList.setListData(plot3DStrings.toArray());
                    ((JTextField)e.getSource()).setText("");
                }
            } );
            plot3DPanel.add(northPanel,BorderLayout.NORTH);
            plot3DPanel.add(new JScrollPane(plot3DList),BorderLayout.CENTER);
            removeButton=new JButton("Remove");
            removeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int ind=plot3DList.getSelectedIndex();
                    if(ind<0) {
                        JOptionPane.showMessageDialog(plot3DList,"You must select a plot to remove.");
                    } else {
                        plot3DStrings.remove(ind);
                        plot3DClasses.remove(ind);
                        plot3DList.setListData(plot3DStrings.toArray());
                    }
                }
            } );
            plot3DPanel.add(removeButton,BorderLayout.SOUTH);
            tabbedPane.add("Plot 3D Styles",plot3DPanel);
            
			JPanel sinkPanel=new JPanel(new BorderLayout());
			northPanel=new JPanel(new BorderLayout());
			classField=new JTextField();
			northPanel.add(classField,BorderLayout.CENTER);
			classField.addActionListener(new ActionListener() {
				@Override
                public void actionPerformed(ActionEvent e) {
					addClass(((JTextField)e.getSource()).getText(),sinkClasses,sinkStrings);
					sinkList.setListData(sinkStrings.toArray());
					((JTextField)e.getSource()).setText("");
				}
			} );
			sinkPanel.add(northPanel,BorderLayout.NORTH);
			sinkPanel.add(sinkList,BorderLayout.CENTER);
			removeButton=new JButton("Remove");
			removeButton.addActionListener(new ActionListener() {
				@Override
                public void actionPerformed(ActionEvent e) {
					int ind=sinkList.getSelectedIndex();
					if(ind<0) {
						JOptionPane.showMessageDialog(sinkList,"You must select a sink to remove.");
					} else {
						sinkStrings.remove(ind);
						sinkClasses.remove(ind);
						sinkList.setListData(sinkStrings.toArray());
					}
				}
			} );
			sinkPanel.add(removeButton,BorderLayout.SOUTH);
			tabbedPane.add("Other Sinks",sinkPanel);
			
			JPanel miscPanel=new JPanel(new BorderLayout());
            northPanel=new JPanel(new GridLayout(8,1));
			JPanel innerPanel=new JPanel(new BorderLayout());
			innerPanel.add(new JLabel("Source Buffer Size in MB"),BorderLayout.WEST);
			innerPanel.add(sourceSizeMB.getTextField(null),BorderLayout.CENTER);
			northPanel.add(innerPanel);
			innerPanel = new JPanel(new BorderLayout());
			innerPanel.add(new JLabel("Number of Threads to use"),BorderLayout.WEST);
			innerPanel.add(numThreads.getTextField(new EditableInt.Listener() {
				@Override
                public void valueChanged() {
					new Thread(new Runnable() {
						@Override
                        public void run() { ThreadHandler.stop(); }
					}).start();
					ThreadHandler.makeInstance(numThreads.getValue());
				}
			}),BorderLayout.CENTER);
			northPanel.add(innerPanel);
			northPanel.add(plottingThreaded.getCheckBox("Plotting Threaded?",null));
            northPanel.add(littleEndian.getCheckBox("Little endian?",null));
            northPanel.add(antialias.getCheckBox("Do antialiasing?",null));
            if(defaultAxisFont==null) {
                makeFontOptions();
            }
            JButton button=new JButton("Set Default Axis Font");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    defaultAxisFont.edit();
                }
            });
            northPanel.add(button);
            button=new JButton("Set Default Label Font");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    defaultLabelFont.edit();
                }
            });
            northPanel.add(button);
            innerPanel=new JPanel(new BorderLayout());
            innerPanel.add(new JLabel("Script Editor Command"),BorderLayout.WEST);
            innerPanel.add(editorCommand.getTextField(null),BorderLayout.CENTER);
            button=new JButton("Browse");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JFileChooser chooser=new JFileChooser();
                    if(chooser.showOpenDialog(dialog)!=JFileChooser.CANCEL_OPTION) {
                        editorCommand.setValue(chooser.getSelectedFile().getAbsolutePath());
                    }
                }
            });
            innerPanel.add(button,BorderLayout.EAST);
            northPanel.add(innerPanel);
			miscPanel.add(northPanel,BorderLayout.NORTH);
			tabbedPane.add("Misc.",miscPanel);
                       
            JPanel windowPanel=new JPanel(new BorderLayout());
            northPanel=new JPanel(new GridLayout(4,1));
            innerPanel=new JPanel(new BorderLayout());
            innerPanel.add(new JLabel("Default Main Window Width"),BorderLayout.WEST);
            innerPanel.add(mainFrameWidth.getTextField(null),BorderLayout.CENTER);
            northPanel.add(innerPanel);
            innerPanel=new JPanel(new BorderLayout());
            innerPanel.add(new JLabel("Default Main Window Height"),BorderLayout.WEST);
            innerPanel.add(mainFrameHeight.getTextField(null),BorderLayout.CENTER);
            northPanel.add(innerPanel);
            innerPanel=new JPanel(new BorderLayout());
            innerPanel.add(new JLabel("Default Plot Window Width"),BorderLayout.WEST);
            innerPanel.add(plotFrameWidth.getTextField(null),BorderLayout.CENTER);
            northPanel.add(innerPanel);
            innerPanel=new JPanel(new BorderLayout());
            innerPanel.add(new JLabel("Default Plot Window Height"),BorderLayout.WEST);
            innerPanel.add(plotFrameHeight.getTextField(null),BorderLayout.CENTER);
            northPanel.add(innerPanel);
            windowPanel.add(northPanel,BorderLayout.NORTH);
            tabbedPane.add("Windows",windowPanel);
                       
            JPanel schemePanel=new JPanel(new BorderLayout());
            final JTextArea textArea=new JTextArea(schemeSource);
            textArea.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    if(!textArea.getText().equals(schemeSource)) {
                        schemeSource=textArea.getText();
                        SVSchemeUtil.resetGlobalEnvironment();
                    }
                }
            });
            schemePanel.add(textArea,BorderLayout.CENTER);
            tabbedPane.add("SVScheme",schemePanel);
            
            JPanel jarPanel=new JPanel(new BorderLayout());
            final JList pathList=new JList(jarPaths.toArray());
            jarPanel.add(pathList,BorderLayout.CENTER);
            northPanel=new JPanel(new GridLayout(1,2));
            button=new JButton("Add");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    addToClassPath();
                    pathList.setListData(jarPaths.toArray());
                }
            });
            northPanel.add(button);
            button=new JButton("Remove");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int index=pathList.getSelectedIndex();
                    if(index<0) return;
                    jarPaths.remove(index);
                    pathList.setListData(jarPaths.toArray());
                    saveClassPath();
                    JOptionPane.showMessageDialog(dialog,"Removed paths will be reflected next time SwiftVis is started.");
                }
            });
            northPanel.add(button);
            jarPanel.add(northPanel,BorderLayout.NORTH);
            tabbedPane.addTab("Class Path",jarPanel);
			
            JPanel scriptPanel=new JPanel(new BorderLayout());
            ScriptEngineManager manager=new ScriptEngineManager();
            List<ScriptEngineFactory> factories=manager.getEngineFactories();
            String[] languages=new String[factories.size()];
            for(int i=0; i<factories.size(); ++i) {
                ScriptEngineFactory sef=factories.get(i);
                languages[i]=sef.getLanguageName();
            }
            engineBox=new JComboBox(languages);
            final JTextArea codeArea=new JTextArea();
            engineBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String language=(String)engineBox.getSelectedItem();
                    String code=getScriptBaseCode(language);
                    if(code!=null) codeArea.setText(code);
                    else codeArea.setText("");
                }
            });
            if(languages.length>0) engineBox.setSelectedIndex(0);
            scriptPanel.add(engineBox,BorderLayout.NORTH);
            codeArea.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    scriptCode.put((String)engineBox.getSelectedItem(),codeArea.getText());
                    saveScript((String)engineBox.getSelectedItem(),codeArea.getText());
                }
            });
            scriptPanel.add(codeArea,BorderLayout.CENTER);
            tabbedPane.add("Scripts",scriptPanel);

            dialog.getContentPane().setLayout(new BorderLayout());
			dialog.getContentPane().add(tabbedPane,BorderLayout.CENTER);
			JPanel buttonPanel=new JPanel(new FlowLayout());
			JButton okButton=new JButton("OK");
			okButton.addActionListener(new ActionListener() {
				@Override
                public void actionPerformed(ActionEvent e) {
					try {
						writeOptions();
					} catch(IOException ex) {
						ex.printStackTrace();
						JOptionPane.showMessageDialog(dialog,"There was an error writing the options to disk.");
					}
					dialog.setVisible(false);
				}
			} );
			buttonPanel.add(okButton);
			dialog.getContentPane().add(buttonPanel,BorderLayout.SOUTH);
			dialog.setSize(600,600);
		}
		return dialog;
	}
	
	@SuppressWarnings("unchecked") 
	public Class<DataSource> selectDataSourceType(Component frame) {
		Object select=JOptionPane.showInputDialog(frame,"Select the type of data file you want to add.","Data Type Selection",
            JOptionPane.QUESTION_MESSAGE,null,sourceStrings.toArray(),sourceStrings.get(0));
        if(select==null) return null;
        try {
	        return (Class<DataSource>)Class.forName(sourceClasses.get(sourceStrings.indexOf(select)));
        } catch(ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

	@SuppressWarnings("unchecked") 
	public Class<Filter> selectFilterType(Component frame) {
		Object select=JOptionPane.showInputDialog(frame,"Select the type of filter you want to add.","Filter Selection",
            JOptionPane.QUESTION_MESSAGE,null,filterStrings.toArray(),filterStrings.get(0));
        if(select==null) return null;
        try {
	        return (Class<Filter>)Class.forName(filterClasses.get(filterStrings.indexOf(select)));
        } catch(ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

	@SuppressWarnings("unchecked") 
	public Class<DataSink> selectSinkType(Component frame) {
        Object select=JOptionPane.showInputDialog(frame,"Select the type of sink you want to add.","Sink Selection",
            JOptionPane.QUESTION_MESSAGE,null,sinkStrings.toArray(),sinkStrings.get(0));
        if(select==null) return null;
        try {
            return (Class<DataSink>)Class.forName(sinkClasses.get(sinkStrings.indexOf(select)));
        } catch(ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

	@SuppressWarnings("unchecked") 
	public Class<DataPlotStyle> selectPlotType(Component frame) {
		Object select=JOptionPane.showInputDialog(frame,"Select the data plot style you want to add.","Plot Selection",
            JOptionPane.QUESTION_MESSAGE,null,plotStrings.toArray(),plotStrings.get(0));
        if(select==null) return null;
        try {
	        return (Class<DataPlotStyle>)Class.forName(plotClasses.get(plotStrings.indexOf(select)));
        } catch(ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

	@SuppressWarnings("unchecked") 
	public Class<Style3D> selectPlot3DType(Component frame) {
        Object select=JOptionPane.showInputDialog(frame,"Select the 3D data plot style you want to add.","Plot Selection",
            JOptionPane.QUESTION_MESSAGE,null,plot3DStrings.toArray(),plot3DStrings.get(0));
        if(select==null) return null;
        try {
            return (Class<Style3D>)Class.forName(plot3DClasses.get(plot3DStrings.indexOf(select)));
        } catch(ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void addClass(String className,ArrayList<String> classVector,ArrayList<String> stringVector) {
        try {
    	    try {
                Method method=Class.forName(className).getMethod("getTypeDescription");
                stringVector.add((String)method.invoke((Object)null));
				classVector.add(className);
        	} catch(NoSuchMethodException e) {
	            e.printStackTrace();
                String desc=JOptionPane.showInputDialog("Class doesn't provide type description, what do you want to call it?");
                stringVector.add(desc);
				classVector.add(className);
        	} catch(IllegalAccessException e) {
	            e.printStackTrace();
        	} catch(IllegalArgumentException e) {
	            e.printStackTrace();
        	} catch(InvocationTargetException e) {
	            e.printStackTrace();
        	}
        } catch(ClassNotFoundException e) {
            System.err.println("No class found with this name: "+className);
			e.printStackTrace();
        }
    }

    public long getSourceSize() {
    	if(sourceSizeMB.getValue()<=0) sourceSizeMB.setValue(100);
        return sourceSizeMB.getValue()*1024L*1024L;
    }
    
    public int getNumThreads() {
    	if(numThreads.getValue()<=0) numThreads.setValue(1);
    	return numThreads.getValue();
    }
    
    public boolean getPlottingThreaded() {
    	return plottingThreaded.getValue();
    }
    
    public boolean getLittleEndian() {
        return littleEndian.getValue();
    }
    
    public boolean getAntialias() {
        return antialias.getValue();
    }
    
    public File getLastDir() {
        if(lastDir==null) {
            lastDir=new File(".");
        }
        return lastDir;
    }
        
    public void setLastDir(File f) {
        lastDir=f;
    }
    
    public static File getSwiftVisDir() {
        String home=System.getenv("HOME");
        File svDir;
        if(home==null) {
            svDir=new File(File.pathSeparator+"SwiftVis");
        } else {
            svDir=new File(new File(home),".SwiftVis");
        }
        if(!svDir.exists()) {
            boolean b=svDir.mkdir();
            if(!b) {
                throw new RuntimeException("Couldn't make SwiftVis directory.");
            }
        }
        return svDir;
    }
    
    public String getSchemeSource() {
        return schemeSource;
    }
    
    public String getScriptBaseCode(String language) {
        String ret=scriptCode.get(language);
        if(ret==null) return "";
        return ret;
    }
    
    public int getMainFrameWidth() {
        return mainFrameWidth.getValue();
    }
    
    public int getMainFrameHeight() {
        return mainFrameHeight.getValue();
    }
    
    public int getPlotFrameWidth() {
        return plotFrameWidth.getValue();
    }
    
    public int getPlotFrameHeight() {
        return plotFrameHeight.getValue();
    }
    
    public Font getAxisFont() {
        if(defaultAxisFont==null) makeFontOptions();
        return defaultAxisFont.getFont();
    }
    
    public Font getLabelFont() {
        if(defaultLabelFont==null) makeFontOptions();
        return defaultLabelFont.getFont();
    }
    
    public String launchEditor(String startText,String extension) {
        String command=editorCommand.getValue();
        if(command.length()<1) {
            command=System.getenv("EDITOR");
        }
        if(command==null) {
            JOptionPane.showMessageDialog(null,"No editor selected.  You need to edit options or set the EDITOR environment variable.");
            return startText;
        }
        try {
            File file=new File("tmp."+extension);
            FileOutputStream fos=new FileOutputStream(file);
            fos.write(startText.getBytes());
            fos.close();
            file.deleteOnExit();
            String[] tmp=command.split(" +");
            String[] cmdArray=new String[tmp.length+1];
            for(int i=0; i<tmp.length; ++i) cmdArray[i]=tmp[i];
            cmdArray[cmdArray.length-1]=file.getAbsolutePath();
            Process proc=Runtime.getRuntime().exec(cmdArray);
            proc.waitFor();
            FileInputStream fis=new FileInputStream(file);
            byte[] buf=new byte[(int)file.length()];
            fis.read(buf);
            startText=new String(buf);
            fis.close();
        } catch(IOException e) {
            JOptionPane.showMessageDialog(null,"IOException working with temporary file.");
            e.printStackTrace();
        } catch(InterruptedException e) {
            JOptionPane.showMessageDialog(null,"There was an exception while the editor was open.");
            e.printStackTrace();
        }
        return startText;
    }
    
	private static OptionsData setup() {
		try {
			ObjectInputStream ois=new ObjectInputStream(new FileInputStream(new File(getSwiftVisDir(),"Options_"+version+".jbf")));
            OptionsData ret=(OptionsData)ois.readObject();
			ois.close();
            return ret;
		} catch(Exception e) {
			System.err.println("Couldn't read options file.  Attempting to create from scratch.");
			try {
                OptionsData ret=new OptionsData();
				ret.sourceClasses=new ArrayList<String>();
                ret.sourceStrings=new ArrayList<String>();
                ret.filterClasses=new ArrayList<String>();
                ret.filterStrings=new ArrayList<String>();
                ret.plotClasses=new ArrayList<String>();
                ret.plotStrings=new ArrayList<String>();
                ret.plot3DClasses=new ArrayList<String>();
                ret.plot3DStrings=new ArrayList<String>();
                ret.sinkClasses=new ArrayList<String>();
                ret.sinkStrings=new ArrayList<String>();
				addClass("edu.swri.swiftvis.sources.BinaryPositionData",ret.sourceClasses,ret.sourceStrings);
                addClass("edu.swri.swiftvis.sources.GeneralData",ret.sourceClasses,ret.sourceStrings);
				addClass("edu.swri.swiftvis.sources.DiscardData",ret.sourceClasses,ret.sourceStrings);
				addClass("edu.swri.swiftvis.sources.DumpSource",ret.sourceClasses,ret.sourceStrings);
                addClass("edu.swri.swiftvis.sources.SPHSource",ret.sourceClasses,ret.sourceStrings);
                addClass("edu.swri.swiftvis.sources.SequenceSource",ret.sourceClasses,ret.sourceStrings);
                addClass("edu.swri.swiftvis.sources.Fixed2DBinnedSource",ret.sourceClasses,ret.sourceStrings);
                addClass("edu.swri.swiftvis.sources.Particle2DBinnedSource",ret.sourceClasses,ret.sourceStrings);
                addClass("edu.swri.swiftvis.sources.CartAndRadSource",ret.sourceClasses,ret.sourceStrings);
                addClass("edu.swri.swiftvis.sources.PKDGRAVSource",ret.sourceClasses,ret.sourceStrings);
                addClass("edu.swri.swiftvis.sources.AutoProcessSource",ret.sourceClasses,ret.sourceStrings);
                addClass("edu.swri.swiftvis.sources.ScriptSource",ret.sourceClasses,ret.sourceStrings);
                addClass("edu.swri.swiftvis.sources.SchemeSource",ret.sourceClasses,ret.sourceStrings);
                addClass("edu.swri.swiftvis.sources.SocketSource",ret.sourceClasses,ret.sourceStrings);

                addClass("edu.swri.swiftvis.filters.SelectFilter",ret.filterClasses,ret.filterStrings);
				addClass("edu.swri.swiftvis.filters.ThinningFilter",ret.filterClasses,ret.filterStrings);
                addClass("edu.swri.swiftvis.filters.CoordConvertFilter",ret.filterClasses,ret.filterStrings);
                addClass("edu.swri.swiftvis.filters.FunctionFilter",ret.filterClasses,ret.filterStrings);
                addClass("edu.swri.swiftvis.filters.BinnedFilter",ret.filterClasses,ret.filterStrings);
                addClass("edu.swri.swiftvis.filters.MovieFilter",ret.filterClasses,ret.filterStrings);
                addClass("edu.swri.swiftvis.filters.MassFilter",ret.filterClasses,ret.filterStrings);
                addClass("edu.swri.swiftvis.filters.SyMBAMassFilter",ret.filterClasses,ret.filterStrings);

                addClass("edu.swri.swiftvis.filters.BoxCarFilter",ret.filterClasses,ret.filterStrings);
                addClass("edu.swri.swiftvis.filters.ClusterFilter",ret.filterClasses,ret.filterStrings);
                addClass("edu.swri.swiftvis.filters.ConstantsFilter",ret.filterClasses,ret.filterStrings);
                addClass("edu.swri.swiftvis.filters.CountRebinFilter",ret.filterClasses,ret.filterStrings);
                addClass("edu.swri.swiftvis.filters.CorrelationFilter",ret.filterClasses,ret.filterStrings);
                addClass("edu.swri.swiftvis.filters.CumulativeFilter",ret.filterClasses,ret.filterStrings);
                addClass("edu.swri.swiftvis.filters.ElementSplitFilter",ret.filterClasses,ret.filterStrings);
                addClass("edu.swri.swiftvis.filters.ElementTableEditor",ret.filterClasses,ret.filterStrings);
                addClass("edu.swri.swiftvis.filters.FrequencyAnalysisFilter",ret.filterClasses,ret.filterStrings);
                addClass("edu.swri.swiftvis.filters.GroupNumberingFilter",ret.filterClasses,ret.filterStrings);
                addClass("edu.swri.swiftvis.filters.InputCollectionFilter",ret.filterClasses,ret.filterStrings);
                addClass("edu.swri.swiftvis.filters.InterpolatedSurfaceFilter",ret.filterClasses,ret.filterStrings);
                addClass("edu.swri.swiftvis.filters.KeySelectionFilter",ret.filterClasses,ret.filterStrings);
                addClass("edu.swri.swiftvis.filters.LinearFitFilter",ret.filterClasses,ret.filterStrings);
                addClass("edu.swri.swiftvis.filters.MatrixFilter",ret.filterClasses,ret.filterStrings);
                addClass("edu.swri.swiftvis.filters.MergeFilter",ret.filterClasses,ret.filterStrings);
                addClass("edu.swri.swiftvis.filters.NonlinearFitFilter",ret.filterClasses,ret.filterStrings);
                addClass("edu.swri.swiftvis.filters.OccultationFilter",ret.filterClasses,ret.filterStrings);
                addClass("edu.swri.swiftvis.filters.PhotometryFilter",ret.filterClasses,ret.filterStrings);
                addClass("edu.swri.swiftvis.filters.RayTraceBinningFilter",ret.filterClasses,ret.filterStrings);
                addClass("edu.swri.swiftvis.filters.RegionSelectionFilter",ret.filterClasses,ret.filterStrings);
                addClass("edu.swri.swiftvis.filters.RotationFilter",ret.filterClasses,ret.filterStrings);
                addClass("edu.swri.swiftvis.filters.SchemeFilter",ret.filterClasses,ret.filterStrings);
                addClass("edu.swri.swiftvis.filters.ScriptFilter",ret.filterClasses,ret.filterStrings);
                addClass("edu.swri.swiftvis.filters.SliceSelectionFilter",ret.filterClasses,ret.filterStrings);
                addClass("edu.swri.swiftvis.filters.SortFilter",ret.filterClasses,ret.filterStrings);
                addClass("edu.swri.swiftvis.filters.StreamBinningFilter",ret.filterClasses,ret.filterStrings);
                addClass("edu.swri.swiftvis.filters.StreamMergeFilter",ret.filterClasses,ret.filterStrings);
                addClass("edu.swri.swiftvis.filters.StreamSplitFilter",ret.filterClasses,ret.filterStrings);
                addClass("edu.swri.swiftvis.filters.StreamUnbinningFilter",ret.filterClasses,ret.filterStrings);
				addClass("edu.swri.swiftvis.filters.WakePeakFilter",ret.filterClasses,ret.filterStrings);

                addClass("edu.swri.swiftvis.sinks.StatSinkListener",ret.sinkClasses,ret.sinkStrings);

                addClass("edu.swri.swiftvis.plot.styles.ScatterStyle",ret.plotClasses,ret.plotStrings);
				addClass("edu.swri.swiftvis.plot.styles.RectangleGridSurface",ret.plotClasses,ret.plotStrings);
                addClass("edu.swri.swiftvis.plot.styles.OrbitStyle",ret.plotClasses,ret.plotStrings);
				addClass("edu.swri.swiftvis.plot.styles.VectorFieldStyle",ret.plotClasses,ret.plotStrings);
                addClass("edu.swri.swiftvis.plot.styles.PolygonStyle",ret.plotClasses,ret.plotStrings);
                addClass("edu.swri.swiftvis.plot.styles.SchemeStyle",ret.plotClasses,ret.plotStrings);
                addClass("edu.swri.swiftvis.plot.styles.ScriptStyle",ret.plotClasses,ret.plotStrings);
                addClass("edu.swri.swiftvis.plot.styles.ImageAdder",ret.plotClasses,ret.plotStrings);
                addClass("edu.swri.swiftvis.plot.styles.AveragedStreamlines",ret.plotClasses,ret.plotStrings);
                addClass("edu.swri.swiftvis.plot.styles.AveragedSurface",ret.plotClasses,ret.plotStrings);
                addClass("edu.swri.swiftvis.plot.styles.IsoLineSurface",ret.plotClasses,ret.plotStrings);
                addClass("edu.swri.swiftvis.plot.styles.BarStyle",ret.plotClasses,ret.plotStrings);
                addClass("edu.swri.swiftvis.plot.styles.HistogramStyle",ret.plotClasses,ret.plotStrings);
                addClass("edu.swri.swiftvis.plot.styles.PiePointsStyle",ret.plotClasses,ret.plotStrings);
                
                addClass("edu.swri.swiftvis.plot.p3d.styles.ScatterStyle3D",ret.plot3DClasses,ret.plot3DStrings);
                addClass("edu.swri.swiftvis.plot.p3d.styles.SurfaceStyle3D",ret.plot3DClasses,ret.plot3DStrings);
                addClass("edu.swri.swiftvis.plot.p3d.styles.TestStyle",ret.plot3DClasses,ret.plot3DStrings);
                ret.writeOptions();
                return ret;
			} catch(IOException e2) {
				System.err.println("Unable to create new options files.");
				e2.printStackTrace();
			}
		}
        return null;
	}

    private void writeOptions() throws IOException {
		ObjectOutputStream oos=new ObjectOutputStream(new FileOutputStream(new File(getSwiftVisDir(),"Options_"+version+".jbf")));
		oos.writeObject(this);
		oos.close();
    }
    
    private void makeFontOptions() {
        class DummyFontUser implements FontUser,Serializable {
            @Override
            public void applyFont(FontOptions options) {}   
            private static final long serialVersionUID=4598625359864568l;
        }
        defaultAxisFont=new FontOptions(new DummyFontUser());
        defaultLabelFont=new FontOptions(new DummyFontUser());
    }
    
    private void addToClassPath() {
        JFileChooser chooser=new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        if(chooser.showOpenDialog(dialog)!=JFileChooser.CANCEL_OPTION) {
            File[] files=chooser.getSelectedFiles();
            try {
                URLClassLoader sysClassLoader=(URLClassLoader)ClassLoader.getSystemClassLoader();
                Class<URLClassLoader> sysClass=URLClassLoader.class;
                Method method=sysClass.getDeclaredMethod("addURL", new Class[]{URL.class});
                method.setAccessible(true);
                for(File f:files) {
                    method.invoke(sysClassLoader,f.toURI().toURL());
                    jarPaths.add(f);
                }
            } catch(InvocationTargetException e) {
                JOptionPane.showMessageDialog(null,"There was an exception adding to the path.");
                e.printStackTrace();
            } catch(NoSuchMethodException e) {
                JOptionPane.showMessageDialog(null,"There was an exception adding to the path.");
                e.printStackTrace();
            } catch(IllegalAccessException e) {
                JOptionPane.showMessageDialog(null,"There was an exception adding to the path.");
                e.printStackTrace();
            } catch(MalformedURLException e) {
                e.printStackTrace();
            }
            saveClassPath();
            ScriptEngineManager manager=new ScriptEngineManager();
            List<ScriptEngineFactory> factories=manager.getEngineFactories();
            String[] languages=new String[factories.size()];
            for(int i=0; i<factories.size(); ++i) {
                ScriptEngineFactory sef=factories.get(i);
                languages[i]=sef.getLanguageName();
            }
            engineBox.setModel(new DefaultComboBoxModel(languages));
        }
    }
    
    private void loadClassPath() {
        File pathFile=new File(getSwiftVisDir(),"ExtraPaths.txt");
        jarPaths=new ArrayList<File>();
        if(pathFile.exists()) {
            try {
                Scanner sc=new Scanner(new FileInputStream(pathFile));
                while(sc.hasNextLine()) {
                    String line=sc.nextLine().trim();
                    if(line.length()>1) {
                        jarPaths.add(new File(line));
                    }
                }
                sc.close();
            } catch(IOException e) {
                JOptionPane.showMessageDialog(null,"Exception loading in path file.");
                e.printStackTrace();
            }
        }
    }
    
    private void loadScripts() {
        scriptCode=new HashMap<String,String>();
        File scriptDir=new File(getSwiftVisDir(),"Scripts");
        if(!scriptDir.exists()) {
            boolean b=scriptDir.mkdir();
            if(!b) {
                System.err.println("Couldn't make scripts directory.");
            }
        }
        File[] scripts=scriptDir.listFiles();
        for(File script:scripts) {
            try {
                byte[] buf=new byte[(int)script.length()];
                FileInputStream fis=new FileInputStream(script);
                fis.read(buf);
                fis.close();
                scriptCode.put(script.getName(),new String(buf));
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void saveClassPath() {
        File pathFile=new File(getSwiftVisDir(),"ExtraPaths.txt");
        try {
            PrintWriter pw=new PrintWriter(new OutputStreamWriter(new FileOutputStream(pathFile)));
            for(File f:jarPaths) {
                pw.println(f.getAbsolutePath());
            }
            pw.close();
        } catch(IOException e) {
            JOptionPane.showMessageDialog(null,"Exception writing path file.");
            e.printStackTrace();
        }
    }
    
    private void saveScript(String language,String code) {
        File scriptDir=new File(getSwiftVisDir(),"Scripts");
        if(!scriptDir.exists()) {
            boolean b=scriptDir.mkdir();
            if(!b) {
                System.err.println("Couldn't make scripts directory.");
            }
        }
        File f=new File(scriptDir,language);
        try {
            FileOutputStream fos=new FileOutputStream(f);
            fos.write(code.getBytes());
            fos.close();
        } catch(IOException e) {
            JOptionPane.showMessageDialog(null,"Exception writing script code.");
            e.printStackTrace();
        }
    }
    
	private static OptionsData inst;

    private ArrayList<String> sourceStrings;
    private ArrayList<String> filterStrings;
    private ArrayList<String> plotStrings;
    private ArrayList<String> plot3DStrings;
	private ArrayList<String> sinkStrings;
    private ArrayList<String> sourceClasses;
    private ArrayList<String> filterClasses;
    private ArrayList<String> plotClasses;
    private ArrayList<String> plot3DClasses;
	private ArrayList<String> sinkClasses;
    
    private EditableInt sourceSizeMB=new EditableInt(500);
    private EditableInt numThreads=new EditableInt(Runtime.getRuntime().availableProcessors());
    private EditableBoolean plottingThreaded=new EditableBoolean(false);
    private EditableBoolean littleEndian=new EditableBoolean(true);
    private EditableBoolean antialias=new EditableBoolean(true);
    private EditableString editorCommand=new EditableString("");
    private String schemeSource="";
    
    private EditableInt mainFrameWidth=new EditableInt(1200);
    private EditableInt mainFrameHeight=new EditableInt(900);
    private EditableInt plotFrameWidth=new EditableInt(800);
    private EditableInt plotFrameHeight=new EditableInt(800);
    
    private FontOptions defaultAxisFont;
    private FontOptions defaultLabelFont;
    
    private transient List<File> jarPaths;
    private transient HashMap<String,String> scriptCode;

    private transient JDialog dialog;
    private transient JComboBox engineBox;
    
    private transient File lastDir;

    public static final String version="0.3.0";
    private static final long serialVersionUID=1957346902l;

}
