/***********************************************************************
 * Copyright (c) 2004 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Actuate Corporation - initial API and implementation
 ***********************************************************************/

package org.eclipse.birt.report.engine.layout.pdf;

import java.util.Iterator;

import org.eclipse.birt.core.format.NumberFormatter;
import org.eclipse.birt.report.engine.content.Dimension;
import org.eclipse.birt.report.engine.content.IAutoTextContent;
import org.eclipse.birt.report.engine.content.IContent;
import org.eclipse.birt.report.engine.content.IPageContent;
import org.eclipse.birt.report.engine.content.IReportContent;
import org.eclipse.birt.report.engine.emitter.IContentEmitter;
import org.eclipse.birt.report.engine.executor.IReportExecutor;
import org.eclipse.birt.report.engine.executor.ReportExecutorUtil;
import org.eclipse.birt.report.engine.extension.IReportItemExecutor;
import org.eclipse.birt.report.engine.internal.executor.dom.DOMReportItemExecutor;
import org.eclipse.birt.report.engine.ir.MasterPageDesign;
import org.eclipse.birt.report.engine.ir.SimpleMasterPageDesign;
import org.eclipse.birt.report.engine.layout.IBlockStackingLayoutManager;
import org.eclipse.birt.report.engine.layout.ILayoutPageHandler;
import org.eclipse.birt.report.engine.layout.PDFConstants;
import org.eclipse.birt.report.engine.layout.area.IArea;
import org.eclipse.birt.report.engine.layout.area.IContainerArea;
import org.eclipse.birt.report.engine.layout.area.impl.AbstractArea;
import org.eclipse.birt.report.engine.layout.area.impl.ContainerArea;
import org.eclipse.birt.report.engine.layout.area.impl.LogicContainerArea;
import org.eclipse.birt.report.engine.layout.area.impl.PageArea;
import org.eclipse.birt.report.engine.layout.area.impl.TableArea;
import org.eclipse.birt.report.engine.layout.content.BlockStackingExecutor;
import org.eclipse.birt.report.engine.layout.pdf.font.FontSplitter;
import org.eclipse.birt.report.engine.layout.pdf.text.Chunk;
import org.eclipse.birt.report.engine.layout.pdf.text.ChunkGenerator;

/**
 * 
 * TODO add multi-column support
 */
public class PDFPageLM extends PDFBlockContainerLM
		implements
			IBlockStackingLayoutManager
{

	final static int DEFAULT_PAGE_WIDTH = 595275;
	final static int DEFAULT_PAGE_HEIGHT = 841889;
	/**
	 * current page area
	 */
	protected PageArea page;

	protected IReportContent report;
	protected IPageContent pageContent;
	protected IReportExecutor reportExecutor = null;
	protected PDFReportLayoutEngine engine;
	protected IContentEmitter emitter;
	protected int pageNumber = 1;

	public PDFPageLM( PDFReportLayoutEngine engine,
			PDFLayoutEngineContext context, IReportContent report,
			IContentEmitter emitter, IReportExecutor executor )
	{
		super( context, null, null, null );
		this.reportExecutor = executor;
		this.engine = engine;
		this.report = report;
		this.emitter = emitter;
	}

	protected void initialize( )
	{
		createRoot( );
		context.setMaxHeight( page.getRoot( ).getHeight( ) );
		context.setMaxWidth( page.getRoot( ).getWidth( ) );
		layoutHeader( );
		layoutFooter( );
		updateBodySize( page );
		context.setMaxHeight( page.getBody( ).getHeight( ) );
		context.setMaxWidth( page.getBody( ).getWidth( ) );
		maxAvaWidth = context.getMaxWidth( );
		//maxAvaHeight = context.getMaxHeight( );
		if(context.pagebreakPaginationOnly())
		{
			maxAvaHeight = Integer.MAX_VALUE;
		}
		else
		{
			maxAvaHeight = context.getMaxHeight( );
		}
		setCurrentIP( 0 );
		setCurrentBP( 0 );
	}

	/**
	 * support body auto resize, remove invalid header and footer
	 * 
	 * @param page
	 */
	protected void updateBodySize( PageArea page )
	{
		IContainerArea header = page.getHeader( );
		ContainerArea footer = (ContainerArea) page.getFooter( );
		ContainerArea body = (ContainerArea) page.getBody( );
		ContainerArea root = (ContainerArea) page.getRoot( );
		if ( header != null && header.getHeight( ) >= root.getHeight( ) )
		{
			page.removeHeader( );
			header = null;
		}
		if ( footer != null && footer.getHeight( ) >= root.getHeight( ) )
		{
			page.removeHeader( );
			footer = null;
		}
		if ( header != null
				&& footer != null
				&& footer.getHeight( ) + header.getHeight( ) >= root
						.getHeight( ) )
		{
			page.removeFooter( );
			page.removeHeader( );
			header = null;
			footer = null;
		}

		body.setHeight( root.getHeight( )
				- ( header == null ? 0 : header.getHeight( ) )
				- ( footer == null ? 0 : footer.getHeight( ) ) );
		body.setPosition( body.getX( ), ( header == null ? 0 : header
				.getHeight( ) ) );
		if ( footer != null )
		{
			footer.setPosition( footer.getX( ), ( header == null ? 0 : header
					.getHeight( ) )
					+ ( body == null ? 0 : body.getHeight( ) ) );
		}
	}

	/**
	 * layout page header area
	 * 
	 */
	protected void layoutHeader( )
	{
		IContent headerContent = pageContent.getPageHeader( );
		IReportItemExecutor headerExecutor = new DOMReportItemExecutor(
				headerContent );
		headerExecutor.execute( );
		PDFRegionLM regionLM = new PDFRegionLM( context, page.getHeader( ),
				headerContent, headerExecutor );
		boolean allowPB = context.allowPageBreak( );
		context.setAllowPageBreak( false );
		regionLM.layout( );
		context.setAllowPageBreak( allowPB );
	}

	/**
	 * layout page footer area
	 * 
	 */
	protected void layoutFooter( )
	{
		IContent footerContent = pageContent.getPageFooter( );
		IReportItemExecutor footerExecutor = new DOMReportItemExecutor(
				footerContent );
		footerExecutor.execute( );
		PDFRegionLM regionLM = new PDFRegionLM( context, page.getFooter( ),
				footerContent, footerExecutor );
		boolean allowPB = context.allowPageBreak( );
		context.setAllowPageBreak( false );
		regionLM.layout( );
		context.setAllowPageBreak( allowPB );
	}

	public void removeHeader( )
	{
		page.removeHeader( );
	}

	public void removeFooter( )
	{
		page.removeFooter( );
	}

	public void floatingFooter( )
	{
		ContainerArea footer = (ContainerArea) page.getFooter( );
		IContainerArea body = page.getBody( );
		IContainerArea header = page.getHeader( );
		if ( footer != null )
		{
			footer.setPosition( footer.getX( ), ( header == null ? 0 : header
					.getHeight( ) )
					+ ( body == null ? 0 : body.getHeight( ) ) );
		}
	}

	public boolean layout( )
	{
		if(!context.isCancel( ))
		{
			boolean childBreak = true;
			startPage( );
			childBreak = layoutChildren( );
			if ( !childBreak )
			{
				isLast = true;
			}
			endPage( );
			return childBreak;
		}
		else
		{
			cancel( );
			return false;
		}
	}

	protected void pageBreakEvent( )
	{
		ILayoutPageHandler pageHandler = engine.getPageHandler( );
		if ( pageHandler != null )
		{
			pageHandler.onPage( this.pageNumber, context );
		}
	}

	protected void startPage( )
	{
		MasterPageDesign pageDesign = getMasterPage( report );
		pageContent = ReportExecutorUtil.executeMasterPage( reportExecutor,
				pageNumber, pageDesign );
		this.content = pageContent;
	}

	protected void endPage( )
	{
		if(context.isAutoPageBreak( ))
		{
			context.setAutoPageBreak( false );
			autoPageBreak( );
		}
		if(isPageEmpty())
		{
			if(!isFirst)
			{
				if ( isLast )
				{
					pageNumber--;
					resolveTotalPage( );
				}
				return;
			}
			else
			{
				if(!isLast)
				{
					return;
				}
			}
		}

		MasterPageDesign mp = getMasterPage( report );

		if ( mp instanceof SimpleMasterPageDesign )
		{
			if ( isFirst
					&& !( (SimpleMasterPageDesign) mp ).isShowHeaderOnFirst( ) )
			{
				removeHeader( );
				isFirst = false;
			}
			if ( isLast
					&& !( (SimpleMasterPageDesign) mp ).isShowFooterOnLast( ) )
			{
				removeFooter( );
			}
			if ( ( (SimpleMasterPageDesign) mp ).isFloatingFooter( ) )
			{
				floatingFooter( );
			}
		}
		if ( isFirst )
		{
			isFirst = false;
		}
		emitter.startPage( pageContent );
		emitter.endPage( pageContent );
		pageBreakEvent( );
		if ( isLast )
		{
			resolveTotalPage( );
		}
		pageNumber++;
	}

	public boolean isPageEmpty( )
	{
		if ( page != null )
		{
			IContainerArea body = page.getBody( );
			if ( body.getChildrenCount( ) > 0 )
			{
				return false;
			}
		}
		return true;
	}

	protected void resolveTotalPage( )
	{
		IContent con = context.getUnresolvedContent( );
		if ( !( con instanceof IAutoTextContent ) )
		{
			return;
		}

		IAutoTextContent totalPageContent = (IAutoTextContent) con;
		if ( null != totalPageContent )
		{
			NumberFormatter nf = new NumberFormatter( );
			String patternStr = totalPageContent.getComputedStyle( )
					.getNumberFormat( );
			nf.applyPattern( patternStr );
			totalPageContent.setText( nf.format( pageNumber ) );

			IArea totalPageArea = null;
			String format = context.getFormat( );
			ChunkGenerator cg = new ChunkGenerator( totalPageContent, true, true, format );
			if ( cg.hasMore( ) )
			{
				Chunk c = cg.getNext( );
				Dimension d = new Dimension(
						(int) ( c.getFontInfo( )
								.getWordWidth( c.getText( ) ) * PDFConstants.LAYOUT_TO_PDF_RATIO ),
						(int) ( c.getFontInfo( ).getWordHeight( ) * PDFConstants.LAYOUT_TO_PDF_RATIO ) );
				totalPageArea = createBlockTextArea( c.getText( ),
						totalPageContent, c.getFontInfo( ), d );
			}
			totalPageContent.setExtension( IContent.LAYOUT_EXTENSION,
					totalPageArea );
			emitter.startAutoText( totalPageContent );
		}
	}

	protected void createRoot( )
	{
		root = new PageArea( pageContent );
		page = (PageArea) root;
		int pageWidth = getDimensionValue( pageContent.getPageWidth( ) );
		int pageHeight = getDimensionValue( pageContent.getPageHeight( ) );
		
		// validate page width
		if ( pageWidth <= 0 )
		{
			pageWidth = DEFAULT_PAGE_WIDTH;
		}

		// validate page height
		if ( pageHeight <= 0 )
		{
			pageHeight = DEFAULT_PAGE_HEIGHT;
		}

		page.setWidth( pageWidth );
		page.setHeight( pageHeight );

		/**
		 * set positon and dimension for root
		 */
		ContainerArea pageRoot = new LogicContainerArea( report );
		pageRoot.setClip( true );
		int rootLeft = getDimensionValue( pageContent.getMarginLeft( ),
				pageWidth );
		int rootTop = getDimensionValue( pageContent.getMarginTop( ), pageWidth );
		rootLeft = Math.max( 0, rootLeft );
		rootLeft = Math.min( pageWidth, rootLeft );
		rootTop = Math.max( 0, rootTop );
		rootTop = Math.min( pageHeight, rootTop );
		pageRoot.setPosition( rootLeft, rootTop );
		int rootRight = getDimensionValue( pageContent.getMarginRight( ),
				pageWidth );
		int rootBottom = getDimensionValue( pageContent.getMarginBottom( ),
				pageWidth );
		rootRight = Math.max( 0, rootRight );
		rootBottom = Math.max( 0, rootBottom );
		if ( rootLeft + rootRight > pageWidth )
		{
			rootRight = 0;
		}
		if ( rootTop + rootBottom > pageHeight )
		{
			rootBottom = 0;
		}
		pageRoot.setWidth( pageWidth - rootLeft - rootRight );
		pageRoot.setHeight( pageHeight - rootTop - rootBottom );
		page.setRoot( pageRoot );

		/**
		 * set position and dimension for header
		 */
		int headerHeight = getDimensionValue( pageContent.getHeaderHeight( ),
				pageRoot.getHeight( ) );
		int headerWidth = pageRoot.getWidth( );
		headerHeight = Math.max( 0, headerHeight );
		headerHeight = Math.min( pageRoot.getHeight( ), headerHeight );
		ContainerArea header = new LogicContainerArea( report);
		header.setHeight( headerHeight );
		header.setWidth( headerWidth );
		header.setPosition( 0, 0 );
		pageRoot.addChild( header );
		page.setHeader( header );

		/**
		 * set position and dimension for footer
		 */
		int footerHeight = getDimensionValue( pageContent.getFooterHeight( ),
				pageRoot.getHeight( ) );
		int footerWidth = pageRoot.getWidth( );
		footerHeight = Math.max( 0, footerHeight );
		footerHeight = Math.min( pageRoot.getHeight( ) - headerHeight,
				footerHeight );
		ContainerArea footer = new LogicContainerArea( report );
		footer.setHeight( footerHeight );
		footer.setWidth( footerWidth );
		footer.setPosition( 0, pageRoot.getHeight( ) - footerHeight );
		pageRoot.addChild( footer );
		page.setFooter( footer );

		/**
		 * set position and dimension for body
		 */
		ContainerArea body = new LogicContainerArea( report );
		int bodyLeft = getDimensionValue( pageContent.getLeftWidth( ), pageRoot
				.getWidth( ) );
		bodyLeft = Math.max( 0, bodyLeft );
		bodyLeft = Math.min( pageRoot.getWidth( ), bodyLeft );
		body.setPosition( bodyLeft, headerHeight );
		int bodyRight = getDimensionValue( pageContent.getRightWidth( ),
				pageRoot.getWidth( ) );
		bodyRight = Math.max( 0, bodyRight );
		bodyRight = Math.min( pageRoot.getWidth( ) - bodyLeft, bodyRight );

		body.setWidth( pageRoot.getWidth( ) - bodyLeft - bodyRight );
		body.setHeight( pageRoot.getHeight( ) - headerHeight - footerHeight );
		page.setBody( body );
		pageRoot.addChild( body );
		body.setClip( true );

		// TODO add left area and right area;

	}

	protected IReportItemExecutor createExecutor( )
	{
		return new BlockStackingExecutor( content, new ReportStackingExecutor(
				reportExecutor ) );
	}

	
	protected void closeLayout( )
	{
		if(context.fitToPage())
		{
			setPageScale();
		}
	}
	
	protected void setPageScale()
	{
		if(page!=null && page.getRoot().getChildrenCount()>0)
		{
			int maxWidth = context.getMaxWidth();
			int maxHeight = context.getMaxHeight();
			int prefWidth = context.getPreferenceWidth();
			int prefHeight = getCurrentBP();
			Iterator iter = page.getBody().getChildren();
			while(iter.hasNext())
			{
				AbstractArea area = (AbstractArea)iter.next();
				if(area instanceof TableArea)
				{
					prefWidth = Math.max(prefWidth, area.getAllocatedWidth());
				}
			}
			
			if(prefHeight>maxHeight)
			{
				((ContainerArea)page.getBody()).setHeight(prefHeight);
				floatingFooter();
			}
			
			if(prefWidth>maxWidth || prefHeight>maxHeight)
			{
				ContainerArea pageRoot = (ContainerArea)page.getRoot();
				float scale = Math.min(maxWidth/(float)prefWidth, maxHeight/(float)prefHeight);
				page.setScale(scale);
				page.setHeight((int)(page.getHeight()/scale));
				page.setWidth((int)(page.getWidth()/scale));
				pageRoot.setPosition((int)(pageRoot.getX()/scale), (int)(pageRoot.getY()/scale));
				pageRoot.setHeight((int)(pageRoot.getHeight()/scale));
				pageRoot.setWidth((int)(pageRoot.getWidth()/scale));
				ContainerArea pageBody = (ContainerArea)page.getBody();
				pageBody.setHeight((int)(pageRoot.getHeight()/scale));
				pageBody.setWidth((int)(pageRoot.getWidth()/scale));
			}
		}
	}

	protected boolean isRootEmpty( )
	{
		if ( page != null )
		{
			IContainerArea body = page.getBody( );
			if ( body.getChildrenCount( ) > 0 || isFirst&&isLast )
			{
				return false;
			}
		}
		return true;
	}
	
}
