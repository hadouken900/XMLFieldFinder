package org.hadouken900;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
/*
Поиск элемента FieldToFind и всех его подэлементов во всех файлах xml, которые лежат в текущей директории.
Для каждого файла xml создает папку с названием аттрибута unid поля noteinfo и в этой папке создает xml файлы содержащие результаты поиска
 */
public class XMLFieldFinder {
    SAXParser parser;
    SearchingXMLHandler handler;
    static StringBuilder tabs = new StringBuilder();
    static String location = System.getProperty("user.dir")+"\\";
    private static boolean isFound;
    static File file;
    static String folderName;
    static FileWriter fw;
    private String FieldToFind;

    public XMLFieldFinder(String FieldToFind) {
        this.FieldToFind = FieldToFind;
    }

    public void init() throws ParserConfigurationException, SAXException {

        System.out.println("Текущая директория: "+ location);

        SAXParserFactory factory = SAXParserFactory.newInstance();
        parser = factory.newSAXParser();

        handler = new SearchingXMLHandler(FieldToFind);
    }


    public void startParse() throws IOException, SAXException {

        File dir = new File(location);
        for (File item : dir.listFiles()) {
            if (item.isFile()) {
                String itemName = item.getName();
                if (itemName.endsWith(".xml")) {
                    System.out.println("Найден файл " + itemName);
                    folderName = itemName.substring(0, itemName.length() - 4);
                    System.out.println("Создаем папку " + folderName);
                    File folder = new File(location + folderName);
                    boolean created = folder.mkdir();
                    if (created || folder.exists()) {
                        System.out.println("Папка "+ folderName+" создана!");
                        System.out.println();
                        parser.parse(new File(location+itemName), handler);
                    }
                }
            }
        }


        if (!isFound)
            System.out.println("Элемент не был найден.");
    }

    private static class SearchingXMLHandler extends DefaultHandler {
        private String element;
        private boolean isEntered;

        public SearchingXMLHandler(String element) {
            this.element = element;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (qName == element) {

                file = new File(location+folderName+"\\abc.xml");
                try {
                    boolean created = file.createNewFile();
                    fw = new FileWriter(file);
                    fw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                    fw.write(element+"\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            if (isEntered) {


                tabs.append("\t");
                try {
                    fw.write(String.format(tabs+ "<%s", qName));

                    int length = attributes.getLength();
                    if (length == 0) {
                        fw.write(">\n");
                    }
                    else {
                        for (int i = 0; i < length; i++){
                            if (qName == "noteinfo" && attributes.getQName(i) == "unid") {
                                String fileName = attributes.getValue(i);
                                fw.flush();
                                fw.close();

                                Path source = Paths.get(file.getPath());
                                Files.move(source,source.resolveSibling(location+folderName+"\\"+fileName+".xml"));
                                file = new File(location+folderName+"\\"+fileName+".xml");
                                fw = new FileWriter(file, true);


                            }
                            fw.write(String.format(" %s =\"%s\"", attributes.getQName(i), attributes.getValue(i)));
                        }

                        fw.write(">\n");

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }

            if (qName.equals(element)) {
                isEntered = true;
                isFound = true;
            }
        }
        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (isEntered) {
                String information = new String(ch, start, length);
                information = information.replace("\n", "").trim();

                if(!information.isEmpty()) {
                    tabs.append("\t");
                    try {
                        fw.write(tabs + information+"\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    tabs.deleteCharAt(tabs.length() - 1);
                }
            }

        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (isEntered) {
                try {
                    fw.write(String.format(tabs+ "</%s>\n", qName));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (tabs.length() != 0)tabs.deleteCharAt(tabs.length()-1);
            }

            if (qName.equals(element)) {
                isEntered = false;

                try {
                    fw.flush();
                    fw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


        }
    }
}
