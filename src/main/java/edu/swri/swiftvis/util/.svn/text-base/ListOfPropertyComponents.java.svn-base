/*
 * Created on Aug 6, 2009
 */
package edu.swri.swiftvis.util;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class ListOfPropertyComponents<T extends ListOfPropertyComponents.PropertiedComponent> extends JPanel {
    public ListOfPropertyComponents(List<T> dl,String borderDesc,final Class<T> type) {
        setBorder(BorderFactory.createTitledBorder(borderDesc));
        dataList=dl;
        setLayout(new GridLayout(2,1));
        list=new JList(dataList.toArray());
        final JPanel propPanel=new JPanel(new GridLayout(1,1));
        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                propPanel.removeAll();
                int index=list.getSelectedIndex();
                if(index>=0) {
                    propPanel.add(dataList.get(index).getPropertiesPanel());
                }
                propPanel.revalidate();
                propPanel.repaint();
            }
        });
        add(new JScrollPane(list));
        JPanel bottomPanel=new JPanel(new BorderLayout());
        JPanel buttonPanel=new JPanel(new GridLayout(1,3));
        JButton button=new JButton("Add");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index=list.getSelectedIndex();
                try {
                    dataList.add(type.newInstance());
                    list.setListData(dataList.toArray());
                    if(index>=0) list.setSelectedIndex(index);
                } catch (InstantiationException ex) {
                    ex.printStackTrace();
                } catch (IllegalAccessException ex) {
                    ex.printStackTrace();
                }
            }
        });
        buttonPanel.add(button);
        button=new JButton("Remove");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index=list.getSelectedIndex();
                if(index<0) return;
                dataList.remove(index);
                list.setListData(dataList.toArray());
            }
        });
        buttonPanel.add(button);
        button=new JButton("Move Up");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index=list.getSelectedIndex();
                if(index<1) return;
                T tmp=dataList.get(index);
                dataList.set(index,dataList.get(index-1));
                dataList.set(index-1,tmp);
                list.setListData(dataList.toArray());
            }
        });
        buttonPanel.add(button);
        bottomPanel.add(buttonPanel,BorderLayout.NORTH);
        bottomPanel.add(new JScrollPane(propPanel),BorderLayout.CENTER);
        add(bottomPanel);
    }
    
    public void dataUpdated() {
        list.setListData(dataList.toArray());
    }
    
    private List<T> dataList;
    private transient JList list;
    private static final long serialVersionUID = 7823261841029897981L;
    
    public interface PropertiedComponent {
        JComponent getPropertiesPanel();
    }
}
