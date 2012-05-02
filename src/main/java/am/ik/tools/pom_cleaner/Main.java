package am.ik.tools.pom_cleaner;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import am.ik.tools.pom_cleaner.jaxb.Model;
import am.ik.tools.pom_cleaner.jaxb.Model.Properties;

import static java.util.regex.Pattern.compile;
import static java.util.regex.Pattern.quote;

public class Main {
    private static Pattern ALLOWED_VERSION_PATTERN = compile(quote("${") + ".+"
            + quote("}"));
    private static String OVERRITE_OPT = "--overwrite";

    /**
     * @param args
     */

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args[0].equals("-h") || args[0].equals("--help")) {
            usage();
            return;
        }
        JAXBContext context = JAXBContext
                .newInstance("am.ik.tools.pom_cleaner.jaxb");
        File pom = new File(args[0]);
        JAXBElement<Model> elm = createJaxbElement(context, pom);
        Model model = elm.getValue();

        Map<String, String> properties = new LinkedHashMap<String, String>();

        try {
            replaceVersion(model.getDependencies().getDependency(), properties);
        } catch (NullPointerException e) {
        }

        try {
            replaceVersion(model.getDependencyManagement().getDependencies()
                    .getDependency(), properties);
        } catch (NullPointerException e) {
        }
        try {
            replaceVersion(model.getBuild().getPlugins().getPlugin(),
                    properties);
        } catch (NullPointerException e) {
        }
        try {
            replaceVersion(model.getBuild().getPluginManagement().getPlugins()
                    .getPlugin(), properties);
        } catch (NullPointerException e) {
        }
        try {
            replaceVersion(model.getBuild().getExtensions().getExtension(),
                    properties);
        } catch (NullPointerException e) {
        }
        try {
            replaceVersion(model.getReporting().getPlugins().getPlugin(),
                    properties);
        } catch (NullPointerException e) {
        }

        addProperties(model.getProperties(), properties);
        OutputStream os = System.out;
        if (args.length > 1 && OVERRITE_OPT.equals(args[1])) {
            os = new BufferedOutputStream(new FileOutputStream(pom));
        }
        outputJaxbElement(context, elm, os);
    }

    private static void usage() {
        System.out.println("java -jar pom-cleaner.jar <path to pom.xml> ["
                + OVERRITE_OPT + "]");
    }

    @SuppressWarnings("unchecked")
    private static JAXBElement<Model> createJaxbElement(JAXBContext context,
            File pom) throws JAXBException {
        Unmarshaller unmarshaller = context.createUnmarshaller();
        JAXBElement<Model> elm = (JAXBElement<Model>) unmarshaller
                .unmarshal(pom);
        return elm;
    }

    private static void outputJaxbElement(JAXBContext context,
            JAXBElement<Model> elm, OutputStream os) throws Exception {
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller
                .setProperty(Marshaller.JAXB_SCHEMA_LOCATION,
                        "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd");
        marshaller.marshal(elm, os);
        if (os instanceof Closeable) {
            ((Closeable) os).close();
        }
    }

    private static String createPropKey(Versionable versionable, boolean append) {
        StringBuilder sb = new StringBuilder();
        String groupId = versionable.getGroupId();
        if (groupId != null && !"".equals(groupId)) {
            sb.append(groupId);
            if (append) {
                sb.append(".");
                sb.append(versionable.getArtifactId());
            }
        } else {
            sb.append(versionable.getArtifactId());
        }
        sb.append(".version");
        return sb.toString();
    }

    private static void replaceVersion(
            List<? extends Versionable> versionables,
            Map<String, String> properties) {

        for (Versionable versionable : versionables) {
            String version = versionable.getVersion();
            if (version == null || version.length() == 0) {
                continue;
            }
            Matcher m = ALLOWED_VERSION_PATTERN.matcher(version);
            if (!m.matches()) {
                String propKey = createPropKey(versionable, false);
                if (properties.containsKey(propKey)
                        && !version.equals(properties.get(propKey))) {
                    propKey = createPropKey(versionable, true);
                }
                properties.put(propKey, version);
                String placeHolder = "${" + propKey + "}";
                versionable.setVersion(placeHolder);

                System.err.println("replace " + propKey + " : " + version
                        + " -> " + placeHolder);
            }
        }
    }

    private static void addProperties(Properties properties,
            Map<String, String> additionals) throws Exception {
        DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docbuilder = dbfactory.newDocumentBuilder();
        Document document = docbuilder.newDocument();

        List<Element> any = properties.getAny();
        for (Entry<String, String> e : additionals.entrySet()) {
            String key = e.getKey();
            Element elm = document.createElementNS(
                    "http://maven.apache.org/POM/4.0.0", key);
            elm.setTextContent(e.getValue());
            any.add(elm);
        }
    }
}
