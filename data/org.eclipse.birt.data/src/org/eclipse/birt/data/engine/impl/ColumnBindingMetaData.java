/*******************************************************************************
 * Copyright (c) 2004, 2009 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.data.engine.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.birt.core.data.DataType;
import org.eclipse.birt.core.data.ExpressionUtil;
import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.api.IBaseQueryDefinition;
import org.eclipse.birt.data.engine.api.IBinding;
import org.eclipse.birt.data.engine.api.IResultMetaData;
import org.eclipse.birt.data.engine.api.querydefn.Binding;
import org.eclipse.birt.data.engine.api.querydefn.QueryDefinition;
import org.eclipse.birt.data.engine.api.querydefn.ScriptExpression;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.i18n.ResourceConstants;
import org.eclipse.birt.data.engine.impl.document.viewing.ExprMetaUtil;
import org.eclipse.birt.data.engine.odi.IResultClass;

/**
 * Result meta data for column binding
 *
 */
public class ColumnBindingMetaData implements IResultMetaData
{

	List<Binding> bindingList;
	Map aliasMap;

	public ColumnBindingMetaData( IBaseQueryDefinition queryDefn,
			IResultClass rsClass ) throws DataException
	{
		bindingList = new ArrayList<Binding>( );
		bindingList.addAll( queryDefn.getBindings( ).values( ) );
		if ( ( (QueryDefinition) queryDefn ).needAutoBinding( )
				&& rsClass != null )
		{
			IResultMetaData metaData = new ResultMetaData( rsClass );
			aliasMap = new HashMap<Integer, String>( );
			int columnCount = metaData.getColumnCount( );
			for ( int i = 0; i < columnCount; i++ )
			{
				int colIndex = i + 1;
				try
				{
					String colName = metaData.getColumnName( colIndex );
					if ( isTemp( colName ) )
					{
						continue;
					}
					
					String alias = metaData.getColumnAlias( colIndex );
					if ( alias != null )
					{
						aliasMap.put( new Integer( colIndex ), alias );
					}

					ScriptExpression baseExpr = new ScriptExpression( ExpressionUtil.createJSDataSetRowExpression( colName ),
							metaData.getColumnType( colIndex ) );
					Binding binding = new Binding( colName, baseExpr );
					binding.setDisplayName( metaData.getColumnLabel( colIndex ) );
					bindingList.add( binding );
				}
				catch ( BirtException e )
				{
				}
			}
		}
	}
	
	
	private boolean isTemp( String name )
	{
		return ( name.matches( "\\Q_{$TEMP_GROUP_\\E\\d*\\Q$}_\\E" )
				|| name.matches( "\\Q_{$TEMP_SORT_\\E\\d*\\Q$}_\\E" )
				|| name.matches( "\\Q_{$TEMP_FILTER_\\E\\d*\\Q$}_\\E" ) || ExprMetaUtil.POS_NAME.equals( name ) );
	}

	/*
	 * @see org.eclipse.birt.data.engine.api.IResultMetaData#getColumnAlias(int)
	 */
	public String getColumnAlias( int index ) throws BirtException
	{
		if ( index <= 0 | index > bindingList.size( ) )
		{
			throw new DataException( ResourceConstants.INVALID_FIELD_INDEX );
		}
		if ( aliasMap == null )
			return null;
		else
			return aliasMap.get( Integer.valueOf( index ) ) == null ? null
					: aliasMap.get( Integer.valueOf( index ) ).toString( );
	}

	/*
	 * @see org.eclipse.birt.data.engine.api.IResultMetaData#getColumnCount()
	 */
	public int getColumnCount( )
	{
		return bindingList.size( );
	}

	/*
	 * @see org.eclipse.birt.data.engine.api.IResultMetaData#getColumnLabel(int)
	 */
	public String getColumnLabel( int index ) throws BirtException
	{
		if ( index <= 0 | index > bindingList.size( ) )
		{
			throw new DataException( ResourceConstants.INVALID_FIELD_INDEX );
		}
		IBinding binding = (IBinding) bindingList.get( index - 1 );
		return binding.getDisplayName( );
	}

	/*
	 * @see org.eclipse.birt.data.engine.api.IResultMetaData#getColumnName(int)
	 */
	public String getColumnName( int index ) throws BirtException
	{
		if ( index <= 0 | index > bindingList.size( ) )
		{
			throw new DataException( ResourceConstants.INVALID_FIELD_INDEX );
		}

		IBinding binding = (IBinding) bindingList.get( index - 1 );
		return binding.getBindingName( );
	}

	/*
	 * @see org.eclipse.birt.data.engine.api.IResultMetaData#getColumnNativeTypeName(int)
	 */
	public String getColumnNativeTypeName( int index ) throws BirtException
	{
		if ( index <= 0 | index > bindingList.size( ) )
		{
			throw new DataException( ResourceConstants.INVALID_FIELD_INDEX );
		}
		return null;
	}

	/*
	 * @see org.eclipse.birt.data.engine.api.IResultMetaData#getColumnType(int)
	 */
	public int getColumnType( int index ) throws BirtException
	{
		if ( index <= 0 | index > bindingList.size( ) )
		{
			throw new DataException( ResourceConstants.INVALID_FIELD_INDEX );
		}

		IBinding binding = (IBinding) bindingList.get( index - 1 );
		return binding.getDataType( );
	}

	/*
	 * @see org.eclipse.birt.data.engine.api.IResultMetaData#getColumnTypeName(int)
	 */
	public String getColumnTypeName( int index ) throws BirtException
	{
		if ( index <= 0 | index > bindingList.size( ) )
		{
			throw new DataException( ResourceConstants.INVALID_FIELD_INDEX );
		}
		IBinding binding = (IBinding) bindingList.get( index - 1 );
		return DataType.getName( binding.getDataType( ) );
	}

	/*
	 * @see org.eclipse.birt.data.engine.api.IResultMetaData#isComputedColumn(int)
	 */
	public boolean isComputedColumn( int index ) throws BirtException
	{
		if ( index <= 0 | index > bindingList.size( ) )
		{
			throw new DataException( ResourceConstants.INVALID_FIELD_INDEX );
		}
		return false;
	}
}