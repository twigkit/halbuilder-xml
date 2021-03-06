package com.theoryinpractise.halbuilder.xml;

import com.theoryinpractise.halbuilder.AbstractRepresentationFactory;
import com.theoryinpractise.halbuilder.api.ContentRepresentation;
import com.theoryinpractise.halbuilder.api.Representation;
import com.theoryinpractise.halbuilder.api.RepresentationException;
import com.theoryinpractise.halbuilder.api.RepresentationReader;
import com.theoryinpractise.halbuilder.impl.representations.ContentBasedRepresentation;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

import static com.theoryinpractise.halbuilder.impl.api.Support.HREF;
import static com.theoryinpractise.halbuilder.impl.api.Support.HREFLANG;
import static com.theoryinpractise.halbuilder.impl.api.Support.NAME;
import static com.theoryinpractise.halbuilder.impl.api.Support.PROFILE;
import static com.theoryinpractise.halbuilder.impl.api.Support.REL;
import static com.theoryinpractise.halbuilder.impl.api.Support.TITLE;
import static com.theoryinpractise.halbuilder.xml.XmlRepresentationFactory.XSI_NAMESPACE;

public class XmlRepresentationReader implements RepresentationReader {
    private AbstractRepresentationFactory representationFactory;

    private XMLOutputter xmlOutputter;

    public XmlRepresentationReader(AbstractRepresentationFactory representationFactory) {
        this.representationFactory = representationFactory;
        this.xmlOutputter = new XMLOutputter();
    }

    public ContentRepresentation read(Reader reader) {
        try {
            Document d = new SAXBuilder().build(reader);
            Element root = d.getRootElement();
            return readRepresentation(root);
        } catch (JDOMException e) {
            throw new RepresentationException(e);
        } catch (IOException e) {
            throw new RepresentationException(e);
        }
    }

    private ContentRepresentation readRepresentation(Element root) {
        String href = root.getAttributeValue("href");

        ContentBasedRepresentation resource = new ContentBasedRepresentation(representationFactory, xmlOutputter.outputString(root) ,href);

        readNamespaces(resource, root);
        readLinks(resource, root);
        readProperties(resource, root);
        readResources(resource, root);

        return resource;
    }

    private void readNamespaces(Representation resource, Element element) {
        List<Namespace> namespaces = element.getAdditionalNamespaces();
        for (Namespace ns : namespaces) {
            if (!"xsi".equals(ns.getPrefix())) {
                resource.withNamespace(ns.getPrefix(), ns.getURI());
            }
        }
    }

    private void readLinks(Representation resource, Element element) {

        List<Element> links = element.getChildren("link");
        for (Element link : links) {
            String rel = link.getAttributeValue(REL);
            String href = link.getAttributeValue(HREF);
            String name = link.getAttributeValue(NAME);
            String title = link.getAttributeValue(TITLE);
            String hreflang = link.getAttributeValue(HREFLANG);
            String profile = link.getAttributeValue(PROFILE);

            resource.withLink(rel, href, name, title, hreflang, profile);
        }

    }

    private void readProperties(Representation resource, Element element) {
        List<Element> properties = element.getChildren();
        for (Element property : properties) {
            if (!property.getName().matches("(link|resource)")) {
                if (property.getAttribute("nil", XSI_NAMESPACE) != null) {
                    resource.withProperty(property.getName(), null);
                } else {
                    resource.withProperty(property.getName(), property.getValue());
                }
            }
        }
    }

    private void readResources(Representation halResource, Element element) {
        List<Element> resources = element.getChildren("resource");
        for (Element resource : resources) {
            String rel = resource.getAttributeValue("rel");
            ContentRepresentation subResource = readRepresentation(resource);
            halResource.withRepresentation(rel, subResource);
        }
    }
}
