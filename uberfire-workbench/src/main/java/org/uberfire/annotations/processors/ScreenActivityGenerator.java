/*
 * Copyright 2012 JBoss Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.uberfire.annotations.processors;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.annotations.processors.exceptions.GenerationException;
import org.uberfire.client.annotations.WorkbenchScreen;

/**
 * A source code generator for Activities
 */
public class ScreenActivityGenerator extends AbstractGenerator {

    private static final Logger logger = LoggerFactory.getLogger( ScreenActivityGenerator.class );

    public StringBuffer generate( final String packageName,
                                  final PackageElement packageElement,
                                  final String className,
                                  final Element element,
                                  final ProcessingEnvironment processingEnvironment ) throws GenerationException {

        logger.debug( "Starting code generation for [" + className + "]" );

        //Extract required information
        final TypeElement classElement = (TypeElement) element;
        final WorkbenchScreen wbw = classElement.getAnnotation( WorkbenchScreen.class );
        final String identifier = wbw.identifier();
        final String onStart0ParameterMethodName = GeneratorUtils.getOnStartZeroParameterMethodName( classElement,
                                                                                                     processingEnvironment );
        final String onStart1ParameterMethodName = GeneratorUtils.getOnStartPlaceRequestParameterMethodName( classElement,
                                                                                                             processingEnvironment );
        final String onMayCloseMethodName = GeneratorUtils.getOnMayCloseMethodName( classElement,
                                                                                    processingEnvironment );
        final String onCloseMethodName = GeneratorUtils.getOnCloseMethodName( classElement,
                                                                              processingEnvironment );
        final String onRevealMethodName = GeneratorUtils.getOnRevealMethodName( classElement,
                                                                                processingEnvironment );
        final String onLostFocusMethodName = GeneratorUtils.getOnLostFocusMethodName( classElement,
                                                                                      processingEnvironment );
        final String onFocusMethodName = GeneratorUtils.getOnFocusMethodName( classElement,
                                                                              processingEnvironment );
        final String getDefaultPositionMethodName = GeneratorUtils.getDefaultPositionMethodName( classElement,
                                                                                                 processingEnvironment );
        final String getTitleMethodName = GeneratorUtils.getTitleMethodName( classElement,
                                                                             processingEnvironment );
        final String getContextIdMethodName = GeneratorUtils.getContextIdMethodName( classElement,
                                                                                     processingEnvironment );
        final ExecutableElement getTitleWidgetMethod = GeneratorUtils.getTitleWidgetMethodName( classElement,
                                                                                                processingEnvironment );
        final String getTitleWidgetMethodName = getTitleWidgetMethod == null ? null : getTitleWidgetMethod.getSimpleName().toString();
        final ExecutableElement getWidgetMethod = GeneratorUtils.getWidgetMethodName( classElement,
                                                                                      processingEnvironment );
        final String getWidgetMethodName = getWidgetMethod == null ? null : getWidgetMethod.getSimpleName().toString();
        final boolean hasUberView = GeneratorUtils.hasUberViewReference( classElement,
                                                                         processingEnvironment,
                                                                         getWidgetMethod );

        final boolean isWidget = GeneratorUtils.getIsWidget( classElement,
                                                             processingEnvironment );
        final String getMenuBarMethodName = GeneratorUtils.getMenuBarMethodName( classElement,
                                                                                 processingEnvironment );
        final String getToolBarMethodName = GeneratorUtils.getToolBarMethodName( classElement,
                                                                                 processingEnvironment );
        final String securityTraitList = GeneratorUtils.getSecurityTraitList( classElement );
        final String rolesList = GeneratorUtils.getRoleList( classElement );

        logger.debug( "Package name: " + packageName );
        logger.debug( "Class name: " + className );
        logger.debug( "Identifier: " + identifier );
        logger.debug( "getContextIdMethodName: " + getContextIdMethodName );
        logger.debug( "onStart0ParameterMethodName: " + onStart0ParameterMethodName );
        logger.debug( "onStart1ParameterMethodName: " + onStart1ParameterMethodName );
        logger.debug( "onMayCloseMethodName: " + onMayCloseMethodName );
        logger.debug( "onCloseMethodName: " + onCloseMethodName );
        logger.debug( "onRevealMethodName: " + onRevealMethodName );
        logger.debug( "onLostFocusMethodName: " + onLostFocusMethodName );
        logger.debug( "onFocusMethodName: " + onFocusMethodName );
        logger.debug( "getDefaultPositionMethodName: " + getDefaultPositionMethodName );
        logger.debug( "getTitleMethodName: " + getTitleMethodName );
        logger.debug( "getTitleWidgetMethodName: " + getTitleWidgetMethodName );
        logger.debug( "getWidgetMethodName: " + getWidgetMethodName );
        logger.debug( "isWidget: " + Boolean.toString( isWidget ) );
        logger.debug( "hasUberView: " + Boolean.toString( hasUberView ) );
        logger.debug( "getMenuBarMethodName: " + getMenuBarMethodName );
        logger.debug( "getToolBarMethodName: " + getToolBarMethodName );
        logger.debug( "securityTraitList: " + securityTraitList );
        logger.debug( "rolesList: " + rolesList );

        //Validate getWidgetMethodName and isWidget
        if ( !isWidget && getWidgetMethodName == null ) {
            throw new GenerationException( "The WorkbenchScreen must either extend IsWidget or provide a @WorkbenchPartView annotated method to return a com.google.gwt.user.client.ui.IsWidget.", packageName + "." + className );
        }
        if ( isWidget && getWidgetMethodName != null ) {
            final String msg = "The WorkbenchScreen both extends com.google.gwt.user.client.ui.IsWidget and provides a @WorkbenchPartView annotated method. The annotated method will take precedence.";
            processingEnvironment.getMessager().printMessage( Kind.WARNING,
                                                              msg );
            logger.warn( msg );
        }

        //Validate onStart0ParameterMethodName and onStart1ParameterMethodName
        if ( onStart0ParameterMethodName != null && onStart1ParameterMethodName != null ) {
            final String msg = "The WorkbenchScreen has methods for both @OnStart() and @OnStart(Place). Method @OnStart(Place) will take precedence.";
            processingEnvironment.getMessager().printMessage( Kind.WARNING,
                                                              msg );
            logger.warn( msg );
        }

        //Validate getTitleMethodName and getTitleWidgetMethodName
        if ( getTitleMethodName == null ) {
            throw new GenerationException( "The WorkbenchScreen must provide a @WorkbenchPartTitle annotated method to return a java.lang.String.", packageName + "." + className );
        }

        //Setup data for template sub-system
        Map<String, Object> root = new HashMap<String, Object>();
        root.put( "packageName",
                  packageName );
        root.put( "className",
                  className );
        root.put( "identifier",
                  identifier );
        root.put( "getContextIdMethodName",
                  getContextIdMethodName );
        root.put( "realClassName",
                  classElement.getSimpleName().toString() );
        root.put( "onStart0ParameterMethodName",
                  onStart0ParameterMethodName );
        root.put( "onStart1ParameterMethodName",
                  onStart1ParameterMethodName );
        root.put( "onMayCloseMethodName",
                  onMayCloseMethodName );
        root.put( "onCloseMethodName",
                  onCloseMethodName );
        root.put( "onRevealMethodName",
                  onRevealMethodName );
        root.put( "onLostFocusMethodName",
                  onLostFocusMethodName );
        root.put( "onFocusMethodName",
                  onFocusMethodName );
        root.put( "getDefaultPositionMethodName",
                  getDefaultPositionMethodName );
        root.put( "getTitleMethodName",
                  getTitleMethodName );
        root.put( "getTitleWidgetMethodName",
                  getTitleWidgetMethodName );
        root.put( "getWidgetMethodName",
                  getWidgetMethodName );
        root.put( "isWidget",
                  isWidget );
        root.put( "hasUberView",
                  hasUberView );
        root.put( "getMenuBarMethodName",
                  getMenuBarMethodName );
        root.put( "getToolBarMethodName",
                  getToolBarMethodName );
        root.put( "securityTraitList",
                  securityTraitList );
        root.put( "rolesList",
                  rolesList );

        //Generate code
        final StringWriter sw = new StringWriter();
        final BufferedWriter bw = new BufferedWriter( sw );
        try {
            final Template template = config.getTemplate( "activityScreen.ftl" );
            template.process( root,
                              bw );
        } catch ( IOException ioe ) {
            throw new GenerationException( ioe );
        } catch ( TemplateException te ) {
            throw new GenerationException( te );
        } finally {
            try {
                bw.close();
                sw.close();
            } catch ( IOException ioe ) {
                throw new GenerationException( ioe );
            }
        }
        logger.debug( "Successfully generated code for [" + className + "]" );

        return sw.getBuffer();
    }

}
