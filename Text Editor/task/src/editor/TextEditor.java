package editor;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MatchResult{
    int start=-1;
    String found=null;
    MatchResult(int start,String found){
        this.start=start;
        this.found=found;
    }
}
public class TextEditor extends JFrame {

    JTextField tbxFind =new JTextField();
    public String getTbxFind(){
        return tbxFind.getText();
    }

    private boolean useRegex=false;
    public boolean isUseRegex(){return useRegex;}
    private void setUseRegex(boolean value){
        useRegex=value;
        chbRegex.setSelected(useRegex);
    }
    private void toggleUseRegex(){
        setUseRegex(!isUseRegex());
    }

    private ArrayList<MatchResult> results=null;
    private int currentResult =0;

    private Matcher matcher=null;

    JButton btnSave=new JButton(new ImageIcon("save.png"));
    JButton btnLoad=new JButton(new ImageIcon("open.png"));
    JButton btnFind=new JButton(new ImageIcon("find.png"));
    JButton btnPrev=new JButton(new ImageIcon("prev.png"));
    JButton btnNext=new JButton(new ImageIcon("next.png"));
    JFileChooser dlgChooseFile=new JFileChooser();
    JCheckBox chbRegex=new JCheckBox("Use regex");

    JTextArea textArea=new JTextArea();
    String getTextArea(){ return textArea.getText();}
    JScrollPane scrollArea=new JScrollPane(textArea);

    private JPanel createTopPanel(){

        btnLoad.setName("OpenButton");
        btnLoad.addActionListener(ActiveEvent-> loadFile());
        btnSave.setName("SaveButton");
        btnSave.addActionListener(ActiveEvent-> saveFile());
        btnFind.setName("StartSearchButton");
        btnFind.addActionListener(ActiveEvent->threadCreateResult());
        btnPrev.setName("PreviousMatchButton");
        btnPrev.addActionListener(ActiveEvent->previousFind());
        btnNext.setName("NextMatchButton");
        btnNext.addActionListener(ActiveEvent->nextFind());
        chbRegex.setName("UseRegExCheckbox");
        chbRegex.addActionListener(ActiveEvent->toggleUseRegex());
        tbxFind.setName("SearchField");

        JPanel topPanel=new JPanel(new BorderLayout());
        JPanel leftPanel=new JPanel(new GridLayout(1,2,5,5));
        JPanel rightPanel=new JPanel(new FlowLayout());
        JPanel rightFindPanel=new JPanel(new GridLayout(1,3,5,5));

        leftPanel.add(btnLoad);
        leftPanel.add(btnSave);

        rightFindPanel.add(btnFind);
        rightFindPanel.add(btnPrev);
        rightFindPanel.add(btnNext);
        rightPanel.add(rightFindPanel);
        rightPanel.add(chbRegex);

        topPanel.add(rightPanel,BorderLayout.EAST);
        topPanel.add(leftPanel,BorderLayout.WEST);
        topPanel.add(tbxFind,BorderLayout.CENTER);

        return  topPanel;
    }

    private JMenu createMenuFile(){
        JMenu menuFile=new JMenu("File");
        JMenuItem menuOpen=new JMenuItem("Open");
        menuOpen.addActionListener(actionEvent -> loadFile());
        JMenuItem menuSave=new JMenuItem("Save");
        menuSave.addActionListener(actionEvent -> saveFile());
        JMenuItem menuExit=new JMenuItem("Exit");
        menuExit.addActionListener(actionEvent -> this.dispose());

        menuFile.setName("MenuFile");
        menuOpen.setName("MenuOpen");
        menuSave.setName("MenuSave");
        menuExit.setName("MenuExit");

        menuFile.add(menuOpen);
        menuFile.add(menuSave);
        menuFile.addSeparator();
        menuFile.add(menuExit);

        return menuFile;
    }
    private JMenu createMenuSearch(){
        JMenu menuSearch=new JMenu("Search");
        JMenuItem menuStartSearch=new JMenuItem("Start search");
        menuStartSearch.addActionListener(actionEvent -> threadCreateResult());
        JMenuItem menuPreviousSearch=new JMenuItem("Previous search");
        menuPreviousSearch.addActionListener(ActiveEvent->previousFind());
        JMenuItem menuNextMatch=new JMenuItem("Next match");
        menuNextMatch.addActionListener(ActiveEvent->nextFind());
        JMenuItem menuUseRegExp=new JMenuItem("Use regular expressions");
        menuUseRegExp.addActionListener(actionEvent -> toggleUseRegex());

        menuSearch.setName("MenuSearch");
        menuStartSearch.setName("MenuStartSearch");
        menuPreviousSearch.setName("MenuPreviousMatch");
        menuNextMatch.setName("MenuNextMatch");
        menuUseRegExp.setName("MenuUseRegExp");

        menuSearch.add(menuStartSearch);
        menuSearch.add(menuPreviousSearch);
        menuSearch.add(menuNextMatch);
        menuSearch.add(menuUseRegExp);

        return  menuSearch;
    }
    private JMenuBar createMenuBar(){
        JMenuBar menuBar= new JMenuBar();
        menuBar.add(createMenuFile());
        menuBar.add(createMenuSearch());
        return menuBar;
    }

    public TextEditor() throws IOException {
        super("The first stage");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(300, 300);
        setLayout(new BorderLayout());

        textArea.setName("TextArea");
        scrollArea.setName("ScrollPane");
        add(scrollArea,BorderLayout.CENTER);

        add(createTopPanel(),BorderLayout.NORTH);
        setJMenuBar(createMenuBar());

        dlgChooseFile.setName("FileChooser");
        add(dlgChooseFile);

        setVisible(true);
    }

    private  static  String loadFileAsString(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()));
    }
    private void saveString(File file,String data) throws IOException {
        try(FileWriter writer=new FileWriter(file)){
            writer.write(data);
        }
    }
    public void loadFile() {
        File chooseFile=null;
        if (dlgChooseFile.showOpenDialog(null)!=JFileChooser.APPROVE_OPTION) return;
        chooseFile=dlgChooseFile.getSelectedFile();
        textArea.setText("");
        try{
            textArea.setText(loadFileAsString(chooseFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveFile()  {
        File chooseFile=null;
        if (dlgChooseFile.showSaveDialog(null)!=JFileChooser.APPROVE_OPTION) return ;
        chooseFile=dlgChooseFile.getSelectedFile();
        try {
            saveString(chooseFile,getTextArea());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void threadCreateResult(){
        Thread thread=new Thread(()->createResults());
        thread.start();
    }

    private ArrayList<MatchResult> getNotRegexResults(String string, String pattern){
        ArrayList<MatchResult> results=new ArrayList<>();
        int start=-1;
        while(true){
            start=string.indexOf(pattern,start+1);
            if (start==-1){
                break;
            }
            results.add(new MatchResult(start,pattern));
        }
        return results;
    }

    private ArrayList<MatchResult> getRegexResults(String string, String pattern){
        matcher=Pattern.compile(pattern).matcher(string);
        ArrayList<MatchResult> results=new ArrayList<>();
        while (matcher.find()){
            results.add(new MatchResult(matcher.start(),matcher.group()));
        }
        return results;

    }

    private ArrayList<MatchResult> createResults(boolean isUseRegex){
        String pattern=getTbxFind();
        String string=getTextArea();
        return isUseRegex? getRegexResults(string,pattern): getNotRegexResults(string,pattern);
    }

    private void createResults(){
        this.results=createResults(isUseRegex());
        if (results.size()>0) {
            setCurrentResultIndex(0);
        }
        else currentResult=-1;
    }

    public void nextFind(){
        if (currentResult==-1) return;
        int newIndex=currentResult+1;
        if (newIndex>=results.size()) newIndex=0;
        setCurrentResultIndex(newIndex);
    }

    public void previousFind(){
        if (currentResult==-1) return;
        int newIndex=currentResult-1;
        if (newIndex<0) newIndex=results.size()-1;
        setCurrentResultIndex(newIndex);
    }

    private  void setCurrentResultIndex(int index){
        currentResult=index;
        setSelection();
    }
    private void setSelection(MatchResult result){
        if (result==null) return;
        textArea.setCaretPosition(result.start + result.found.length());
        textArea.select(result.start, result.start + result.found.length());
        textArea.grabFocus();
    }

    private void setSelection(){
        if (currentResult<0) return;
        setSelection(results.get(currentResult));
    }

    public static void main(String[] args) throws IOException {
        TextEditor editor=new TextEditor();
    }
}

