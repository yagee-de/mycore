package org.mycore.frontend.editor.validation;

import java.io.ByteArrayOutputStream;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.transform.JDOMSource;
import org.mycore.common.MCRConstants;

public class MCRXSLConditionValidator extends MCRValidator {

    @Override
    public boolean hasRequiredPropertiesForValidation() {
        return hasProperty("xsl");
    }

    @Override
    public boolean isValid(String input) throws Exception {
        Document xsl = buildXSLStylesheet();
        Document xml = buildInputDocument(input);
        String output = transform(xml, xsl);
        return "true".equals(output);
    }

    private Document buildXSLStylesheet() {
        Element stylesheet = new Element("stylesheet").setAttribute("version", "1.0");
        stylesheet.setNamespace(MCRConstants.XSL_NAMESPACE);

        for (Namespace ns : MCRConstants.getStandardNamespaces()) {
            if (!ns.equals(MCRConstants.XSL_NAMESPACE)) {
                stylesheet.addNamespaceDeclaration(ns);
            }
        }

        Element output = new Element("output", MCRConstants.XSL_NAMESPACE);
        output.setAttribute("method", "text");
        output.setAttribute("encoding", "UTF-8");
        stylesheet.addContent(output);

        Element template = new Element("template", MCRConstants.XSL_NAMESPACE);
        template.setAttribute("match", "/input");
        stylesheet.addContent(template);

        Element choose = new Element("choose", MCRConstants.XSL_NAMESPACE);
        template.addContent(choose);

        Element when = new Element("when", MCRConstants.XSL_NAMESPACE);
        when.setAttribute("test", getProperty("xsl"));
        when.addContent("true");
        choose.addContent(when);

        Element otherwise = new Element("otherwise", MCRConstants.XSL_NAMESPACE);
        otherwise.addContent("false");
        choose.addContent(otherwise);

        return new Document(stylesheet);
    }

    private Document buildInputDocument(String input) {
        return new Document(new Element("input").addContent(input));
    }

    private String transform(Document input, Document xsl) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer(new JDOMSource(xsl));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        transformer.transform(new JDOMSource(input), new StreamResult(out));
        out.close();
        return out.toString();
    }
}
