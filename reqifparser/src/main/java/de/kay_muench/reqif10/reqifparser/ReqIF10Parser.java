/**
 * Copyright (c) 2014 Kay Erik Münch.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.spdx.org/licenses/EPL-1.0
 *
 * Contributors:
 *     Kay Erik Münch - initial API and implementation
 *
 */
package de.kay_muench.reqif10.reqifparser;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource.Diagnostic;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xml.namespace.XMLNamespacePackage;
import org.eclipse.rmf.reqif10.ReqIF;
import org.eclipse.rmf.reqif10.ReqIF10Package;
import org.eclipse.rmf.reqif10.datatypes.DatatypesPackage;
import org.eclipse.rmf.reqif10.serialization.ReqIF10ResourceFactoryImpl;
import org.eclipse.rmf.reqif10.xhtml.XhtmlPackage;
import org.eclipse.sphinx.emf.serialization.XMLPersistenceMappingResourceSetImpl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ReqIF10Parser {
	private String reqIFFilename;
	private InputStream reqifStream;
	private List<String> diagnostics = new ArrayList<String>();
	private boolean removeToolExtensions = false;
	private boolean removeTemporaries = true;

	public ReqIF parseReqIFContent() {
		registerEPackageStd();
		ReqIF reqif;

        String wkFile;
        if (getReqifStream() == null) {
            wkFile = this.getReqIFFilename();
            if (isRemoveToolExtensions()) {
                ToolExRemover remover = new ToolExRemover();
                remover.setDeleteOnExit(this.isRemoveTemporaries());
                wkFile = remover.remove(this.getReqIFFilename());
            }
            reqif = this.parse(wkFile);
        } else {
            reqif = parse(getReqifStream());
        }

        return reqif;
	}

	public String getReqIFFilename() {
		return reqIFFilename;
	}

	public void setReqIFFilename(String filename) {
		if (filename != null) {
			reqIFFilename = filename;
		}
	}

	public boolean isRemoveToolExtensions() {
		return removeToolExtensions;
	}

	public void setRemoveToolExtensions(boolean removeToolExtensions) {
		this.removeToolExtensions = removeToolExtensions;
	}

	public boolean isRemoveTemporaries() {
		return removeTemporaries;
	}

	public void setRemoveTemporaries(boolean removeTemporaries) {
		this.removeTemporaries = removeTemporaries;
	}

    public InputStream getReqifStream() {
        return reqifStream;
    }

    public void setReqifStream(InputStream reqifStream) {
        this.reqifStream = reqifStream;
    }

    private ReqIF parse(final String fileName) {
        try {
            URI uri = URI.createFileURI(fileName);
            XMLResource resource = loadResourceFromUri(uri);
            return parseReqif(resource);
        } catch (IOException e) {
            throw new RuntimeException("Parsing '" + fileName + "' failed.", e.getCause());
        }
    }

    private ReqIF parse(final InputStream stream) {
        try {
            URI uri = URI.createFileURI("data:///input.stream");
            XMLResource resource = loadResourceFromStream(uri, stream);
            return parseReqif(resource);
        } catch (IOException e) {
            throw new RuntimeException("Parsing InputStream failed.", e.getCause());
        }
    }

    private XMLResource loadResourceFromStream(URI uri, InputStream stream) throws IOException {
        ResourceFactoryImpl resourceFactory = new ReqIF10ResourceFactoryImpl();
        XMLResource resource = (XMLResource) resourceFactory.createResource(uri);
        Map<?, ?> options = null;
        resource.load(stream, options);
        return resource;
    }

    private XMLResource loadResourceFromUri(URI uri) throws IOException {
        ResourceFactoryImpl resourceFactory = new ReqIF10ResourceFactoryImpl();
        XMLResource resource = (XMLResource) resourceFactory.createResource(uri);
        Map<?, ?> options = null;
        resource.load(options);
        return resource;
    }

    private ReqIF parseReqif(XMLResource resource) {
        this.getDiagnostics().clear();
        for (Diagnostic d : resource.getErrors()) {
            this.getDiagnostics().add("ERROR " + d.getLocation() + " " + d.getLine() + " " + d.getMessage());
        }
        for (Diagnostic d : resource.getWarnings()) {
            this.getDiagnostics().add("WARNING " + d.getLocation() + " " + d.getLine() + " " + d.getMessage());
        }

        ResourceSet resourceSet = new XMLPersistenceMappingResourceSetImpl();
        resourceSet.getResources().add(resource);

        EList<EObject> rootObjects = resource.getContents();
        if (rootObjects.isEmpty()) {
            throw new RuntimeException("The resource is to be empty.");
        }
        ReqIF reqif = (ReqIF) rootObjects.get(0);

        return reqif;
    }

    private final void registerEPackageStd() {
		EPackage.Registry.INSTANCE.clear();
		EPackage.Registry.INSTANCE.put(ReqIF10Package.eNS_URI,
				ReqIF10Package.eINSTANCE);
		EPackage.Registry.INSTANCE.put(XhtmlPackage.eNS_URI,
				XhtmlPackage.eINSTANCE);
		EPackage.Registry.INSTANCE.put(DatatypesPackage.eNS_URI,
				DatatypesPackage.eINSTANCE);
		EPackage.Registry.INSTANCE.put(XMLNamespacePackage.eNS_URI,
				XMLNamespacePackage.eINSTANCE);
	}

	public List<String> getDiagnostics() {
		return diagnostics;
	}
}
