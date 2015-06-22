package com.fpg.ec.codegen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import com.fpg.ec.codegen.model.CodeGenConfig;
import com.fpg.ec.codegen.model.Table;

public class codeGenerator {

	static CodeGenConfig codeGenConfig = new CodeGenConfig();
	static boolean isHasDataField = false;//判斷是否有BLOB OR CLOB欄位
	
	public static final HashMap<Integer, String> MSSQL_SQLTYPE_NMMAP = new HashMap<Integer, String>() {
		private static final long serialVersionUID = 3872044183611831556L;
		{
			put(java.sql.Types.BLOB, "BLOB");
			put(java.sql.Types.CLOB, "CLOB");
			put(java.sql.Types.DATE, "DATE");
			put(java.sql.Types.NUMERIC, "NUMERIC");
			put(java.sql.Types.DECIMAL, "DECIMAL");
			put(java.sql.Types.NVARCHAR, "NVARCHAR");
			put(java.sql.Types.VARCHAR, "VARCHAR");
			put(java.sql.Types.BINARY, "BINARY");
			put(java.sql.Types.VARBINARY, "VARBINARY");
		}
	};
	
	public static final HashMap<Integer, String> ORACLE_SQLTYPE_NMMAP = new HashMap<Integer, String>() {
		private static final long serialVersionUID = 3872044183611831556L;
		{
			//ORACLE
			put(1111, "NVARCHAR");
			put(2004, "BLOB");
			put(2005, "CLOB");
			put(3, "NUMERIC");
		}
	};

	
	public static void main(String[] args) {
		try {
			System.out.println("Generation Start...");

			codeGenConfig = getCodeGenConfig();//讀取XML
			
			//建立目錄
			genFolder();
			
			dbDataReceive();//取得DB TABLE SCHEMA

			System.out.println("Generation End...");
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static CodeGenConfig getCodeGenConfig() throws Exception{
		//讀入XML
		File configFile = new File("src/com/fpg/ec/codegen/xml/codeGeneratorConfig.xml");
		FileInputStream fi = new FileInputStream(configFile);
		
		BufferedReader br = new BufferedReader(new InputStreamReader(fi, "UTF-8"));
		StringBuffer strBufXml = new StringBuffer();
		String strTemp = "";
		while ((strTemp = br.readLine()) != null) {
			strBufXml.append(strTemp);
		}
		br.close();
		
		StringReader stringReader = new StringReader(strBufXml.toString());
		JAXBContext jaxbContext = JAXBContext.newInstance(CodeGenConfig.class);
		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
		CodeGenConfig codeGenConfigLocal = (CodeGenConfig)unmarshaller.unmarshal(stringReader);
		
		return codeGenConfigLocal;
	}
	
	public static void genFolder() throws Exception{
		//建立Package路徑
        File filePath = new File("./src/" + codeGenConfig.getObject().getPackage().replaceAll("\\.", "\\/").toLowerCase());
		if(!filePath.exists()){
			filePath.mkdirs();
		}
		
		//建立MODEL
		if(codeGenConfig.getObject().getCreateModel().equals("Y")){
	        filePath = new File("./src/" + codeGenConfig.getObject().getPackage().replaceAll("\\.", "\\/").toLowerCase() + "/model/");
			if(!filePath.exists()){
				filePath.mkdirs();
			}
		}
	}

	public static void dbDataReceive() {
		Connection conn = null;
		ResultSet rs = null;
		Hashtable<String, ArrayList<String>> hstTableByColName = new Hashtable<String, ArrayList<String>>();//儲存Table ColumnName名稱
		Hashtable<String, ArrayList<String>> hstTableByColType = new Hashtable<String, ArrayList<String>>();//儲存Table ColumnType名稱
		Hashtable<String, ArrayList<String>> hstTableByColSize = new Hashtable<String, ArrayList<String>>();//儲存Table ColumnLength
		Hashtable<String, ArrayList<String>> hstTableByColFraSize = new Hashtable<String, ArrayList<String>>();//儲存Table Column小數Length
		Hashtable<String, ArrayList<String>> hstTableByDetial = new Hashtable<String, ArrayList<String>>();//儲存Table 附屬資料名稱
		
		Hashtable<String, String> hstTableByDetialObject = new Hashtable<String, String>();//儲存Table 附屬資料物件對應TABLE名稱
		Hashtable<String, String> hstTableByObjName = new Hashtable<String, String>();//儲存Table 物件名稱

		try {
			
			//取得DB SCHEMA
			String dbDriver = "";
			if(codeGenConfig.getDbConnection().getType().equals("MSSQL")){
				dbDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
			}else{
				dbDriver = "oracle.jdbc.OracleDriver";
			}
			
			Class.forName(dbDriver);
			conn = DriverManager.getConnection(codeGenConfig.getDbConnection().getUrl(), codeGenConfig.getDbConnection().getId(), codeGenConfig.getDbConnection().getPwd());
			
			if(codeGenConfig.getTables().getTable().size() > 0){
				DatabaseMetaData dbmd = null;
				ArrayList<String> alTableColumnName = null;		//ColumnName名稱
				ArrayList<String> alTableColumnType = null;		//ColumnType名稱
				ArrayList<String> alTableColumnSize = null;		//ColumnLength
				ArrayList<String> alTableColumnFraSize = null;	//Column小數Length
				
				for(Table table : codeGenConfig.getTables().getTable()){
					alTableColumnName = new ArrayList<String>();
					alTableColumnType = new ArrayList<String>();
					alTableColumnSize = new ArrayList<String>();
					alTableColumnFraSize = new ArrayList<String>();
					
					System.out.println("Table Name:" + table.getTableName());
					dbmd = conn.getMetaData();
					String theTableName = table.getTableName();
					String theObjectName = table.getObjectName();	
					
					rs = dbmd.getColumns(null, null, theTableName.toUpperCase(), null);
					while (rs.next()) {
					
						alTableColumnName.add(rs.getString("COLUMN_NAME"));
						if(codeGenConfig.getDbConnection().getType().equals("MSSQL")){
							alTableColumnType.add(MSSQL_SQLTYPE_NMMAP.get(rs.getInt("DATA_TYPE")));
						}else{
							alTableColumnType.add(ORACLE_SQLTYPE_NMMAP.get(rs.getInt("DATA_TYPE")));
						}
						
						alTableColumnSize.add(rs.getString("COLUMN_SIZE"));
					
						if(rs.getString("DECIMAL_DIGITS") != null){
							alTableColumnFraSize.add(rs.getString("DECIMAL_DIGITS"));
						}else{
							alTableColumnFraSize.add("0");
						}
						
					}

					if(table.getHasMaster().length() > 0){
						if(hstTableByDetial.containsKey(table.getHasMaster())){
							ArrayList<String> alTableDetialHt = (ArrayList<String>)hstTableByDetial.get(table.getHasMaster());
							alTableDetialHt.add(theObjectName);
							hstTableByDetialObject.put(theObjectName, table.getTableName());
						}else{
							ArrayList<String> alTableDetialNew = new ArrayList<String>();
							alTableDetialNew.add(theObjectName);
							hstTableByDetial.put(table.getHasMaster(), alTableDetialNew);
							hstTableByDetialObject.put(theObjectName, table.getTableName());
						}
					}

					hstTableByColName.put(theTableName, alTableColumnName);
					hstTableByColType.put(theTableName, alTableColumnType);
					hstTableByColSize.put(theTableName, alTableColumnSize);
					hstTableByColFraSize.put(theTableName, alTableColumnFraSize);
					hstTableByObjName.put(theTableName, table.getObjectName());
				}
			}

			//取得物件TABLE資料
			Set<String> set = hstTableByObjName.keySet();
			Iterator<String> itr = set.iterator();
			while(itr.hasNext()){
				String key = itr.next();
				String theTableName = key;
				String theObjectName = (String)hstTableByObjName.get(key);
				ArrayList<String> alTableColumnName = (ArrayList<String>)hstTableByColName.get(key);//ColumnName名稱
				ArrayList<String> alTableColumnType = (ArrayList<String>)hstTableByColType.get(key);//ColumnType名稱
				ArrayList<String> alTableColumnSize = (ArrayList<String>)hstTableByColSize.get(key);//ColumnLength
				ArrayList<String> alTableColumnFraSize = (ArrayList<String>)hstTableByColFraSize.get(key);//Column小數Length
				ArrayList<String> alTableDetial = (ArrayList<String>)hstTableByDetial.get(key);//Table 附屬屬資料名稱
				
				//建立MODEL
				if(codeGenConfig.getObject().getCreateModel().equals("Y")){
					//產生ModelBase物件
					generateModelBase(theTableName, theObjectName, alTableColumnName, alTableColumnType);
						
					//產生Model物件
					generateModel(theTableName, theObjectName, alTableDetial, hstTableByDetialObject);

					//產生ModelCondition物件
					generateModelCondition(theTableName, theObjectName, alTableColumnName);
					
					//產生ModelList物件
					generateModelList(theTableName, theObjectName);
					
					//產生ModelValidate物件
					generateModelValidate(theTableName, theObjectName, alTableColumnName, alTableColumnType, alTableColumnSize, alTableColumnFraSize);
				}
				
				//建立DAO
				if(codeGenConfig.getObject().getCreateDao().equals("Y")){
					//產生DAO Interface物件
					generateDAOInterface(theTableName, theObjectName);
				}
				
				//建立DAO Mapper
				if(codeGenConfig.getObject().getCreateMapper().equals("Y")){
					//產生DAO Mapper XML
					generateDAOMapper(theTableName, theObjectName,codeGenConfig.getDbConnection().getType().toLowerCase(), alTableColumnName, alTableColumnType);
				}

			}
			
			//建立BO
			if(codeGenConfig.getObject().getCreateBo().equals("Y")){
				//產生BO Interface物件
				generateBOInterface(codeGenConfig.getObject().getModule(), hstTableByObjName);
				
				//產生BO Implement物件
				generateBOImplement(codeGenConfig.getObject().getModule(), hstTableByObjName, hstTableByDetial, hstTableByDetialObject);
				
				//產生UtilBO Interface物件
				generateUtilBOInterface(codeGenConfig.getObject().getModule(), hstTableByObjName);
				
				//產生UtilBO Implement物件
				generateUtilBOImplement(codeGenConfig.getObject().getModule(), hstTableByObjName);
			}

			//建立UtilDAO
			if(codeGenConfig.getObject().getCreateDao().equals("Y")){
				//產生UtilDAO Interface物件
				generateUtilDAOInterface(codeGenConfig.getObject().getModule(), hstTableByObjName);
			}
			
			//建立UtilDAO Mapper
			if(codeGenConfig.getObject().getCreateMapper().equals("Y")){
				//產生UtilDAO Mapper XML
				generateUtilDAOMapper(codeGenConfig.getObject().getModule(), codeGenConfig.getDbConnection().getType().toLowerCase(), hstTableByObjName, hstTableByColName, hstTableByColType);
			}
			
			
			//建立able Property
			if(codeGenConfig.getObject().getCreateTableProperty().equals("Y")){
				//產生Table Property
				generateTableProperty(codeGenConfig.getObject().getModule(), hstTableByObjName);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch (Exception e) {
			}

		}
	}
	
	/**
	* 產生ModelBase
	*/
	public static void generateModelBase(String theTableName, String theObjectName, ArrayList<String> alTableColumnName,  ArrayList<String> alTableColumnType) throws Exception {
		//建立Package路徑
        File filePath = new File("./src/" + codeGenConfig.getObject().getPackage().replaceAll("\\.", "\\/") + "/model/base/");
		if(!filePath.exists()){
			filePath.mkdirs();
		}

		//建立ModelBase物件
	    if(alTableColumnName != null && alTableColumnName.size() > 0 && alTableColumnType != null && alTableColumnType.size() > 0){
	    	String context = readTemplateFile("ModelBaseTemplate.txt");
	        StringBuffer contextSbr = new StringBuffer();
            String ObjectFileName = "./src/" + codeGenConfig.getObject().getPackage().replaceAll("\\.", "\\/").toLowerCase() + "/model/base/" + theObjectName + "Base.java";
            if(!"".equals(context)){
            	context = context.replaceAll("<<Author>>", codeGenConfig.getObject().getAuthor());	
            	context = context.replaceAll("<<PackageName>>", codeGenConfig.getObject().getPackage().toLowerCase());	
            	context = context.replaceAll("<<ObjectName>>", theObjectName);
            	context = context.replaceAll("<<TableName>>", theTableName);
            	for(int i=0; i<alTableColumnName.size(); i++){
            		contextSbr.append(genModelGetterSetter(alTableColumnName.get(i).toString(), alTableColumnType.get(i).toString()));
            	}
            	
            	context = context.replaceAll("<<GetSetMethod>>", contextSbr.toString());
	        }
	            
	        FileWriter fileOut = new FileWriter(ObjectFileName, false);
	        fileOut.write(context);
	        fileOut.flush();
	        fileOut.close();
	    }
	    
	}
	
	/**
	* 產生Model
	*/
	public static void generateModel(String theTableName, String theObjectName,  ArrayList<String> alTableDetial, Hashtable<String, String> hstTableByDetialObject) throws Exception {
		//建立Package路徑
        File filePath = new File("./src/" + codeGenConfig.getObject().getPackage().replaceAll("\\.", "\\/") + "/model/");
		if(!filePath.exists()){
			filePath.mkdirs();
		}

		//建立Model物件
    	String context = readTemplateFile("ModelTemplate.txt");
        String ObjectFileName = "./src/" + codeGenConfig.getObject().getPackage().replaceAll("\\.", "\\/").toLowerCase() + "/model/" + theObjectName + ".java";
        if(!"".equals(context)){
        	context = context.replaceAll("<<Author>>", codeGenConfig.getObject().getAuthor());	
        	context = context.replaceAll("<<PackageName>>", codeGenConfig.getObject().getPackage().toLowerCase());	
        	context = context.replaceAll("<<ObjectName>>", theObjectName);
        	context = context.replaceAll("<<TableName>>", theTableName);
        	
        	if(alTableDetial != null && alTableDetial.size() > 0){
        		context = context.replaceAll("<<DeatailListImportData>>", genDeatailListImportData(alTableDetial, codeGenConfig.getObject().getPackage().toLowerCase()));
        		context = context.replaceAll("<<DeatailList>>", genDeatailListDef(alTableDetial, theTableName, hstTableByDetialObject));
        	}else{
        		context = context.replaceAll("<<DeatailListImportData>>", "");
        		context = context.replaceAll("<<DeatailList>>", "");
        	}
        }
            
        FileWriter fileOut = new FileWriter(ObjectFileName, false);
        fileOut.write(context);
        fileOut.flush();
        fileOut.close();
	   
	}
	
	/**
	* 產生Model List
	*/
	public static void generateModelList(String theTableName, String theObjectName) throws Exception {
		//建立Package路徑
        File filePath = new File("./src/" + codeGenConfig.getObject().getPackage().replaceAll("\\.", "\\/") + "/model/list/");
		if(!filePath.exists()){
			filePath.mkdirs();
		}

		//建立Model物件
    	String context = readTemplateFile("ModelListTemplate.txt");
        String ObjectFileName = "./src/" + codeGenConfig.getObject().getPackage().replaceAll("\\.", "\\/").toLowerCase() + "/model/list/" + theObjectName + "List.java";
        if(!"".equals(context)){
        	context = context.replaceAll("<<Author>>", codeGenConfig.getObject().getAuthor());
        	context = context.replaceAll("<<TableName>>", theTableName);
        	context = context.replaceAll("<<PackageName>>", codeGenConfig.getObject().getPackage().toLowerCase());	
        	context = context.replaceAll("<<ObjectName>>", theObjectName);
        }
            
        FileWriter fileOut = new FileWriter(ObjectFileName, false);
        fileOut.write(context);
        fileOut.flush();
        fileOut.close();
	}

	/**
	* 產生Model Condition
	*/
	public static void generateModelCondition(String theTableName, String theObjectName, ArrayList<String> alTableColumnName) throws Exception {
		//建立Package路徑
        File filePath = new File("./src/" + codeGenConfig.getObject().getPackage().replaceAll("\\.", "\\/") + "/model/condition/");
		if(!filePath.exists()){
			filePath.mkdirs();
		}

		//建立ModelBase物件
	    if(alTableColumnName != null && alTableColumnName.size() > 0){
	    	String context = readTemplateFile("ModelConditionTemplate.txt");
            String ObjectFileName = "./src/" + codeGenConfig.getObject().getPackage().replaceAll("\\.", "\\/").toLowerCase() + "/model/condition/" + theObjectName + "Condition.java";
            if(!"".equals(context)){
            	context = context.replaceAll("<<Author>>", codeGenConfig.getObject().getAuthor());	
            	context = context.replaceAll("<<PackageName>>", codeGenConfig.getObject().getPackage().toLowerCase());	
            	context = context.replaceAll("<<ObjectName>>", theObjectName);
            	context = context.replaceAll("<<TableName>>", theTableName);
            	context = context.replaceAll("<<SearhDateField>>", genSearhDateField(alTableColumnName, theTableName));
            	context = context.replaceAll("<<SearhArrayField>>", genSearhArrayField(alTableColumnName, theTableName));
            	context = context.replaceAll("<<OrderByField>>", genOrderByField(alTableColumnName, theTableName));
            	context = context.replaceAll("<<CusSearhField>>", genCusSearhField(alTableColumnName, theTableName));
	        }
	            
	        FileWriter fileOut = new FileWriter(ObjectFileName, false);
	        fileOut.write(context);
	        fileOut.flush();
	        fileOut.close();
	    }
	    
	}
	
	/**
	 * 讀取template檔案, 建立template string
	 */
	public static String readTemplateFile(String templateName) throws Exception {
	    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new java.io.File("./template/" + templateName)), "utf8"));
	    String strTmp = reader.readLine();
	    StringBuffer strbTmp = new StringBuffer();
	    while (strTmp != null) {
	        strbTmp.append(strTmp);
	        strbTmp.append("\n");
	        strTmp = reader.readLine();
	    }      
	    reader.close();
	    
	    return strbTmp.toString();
	}
	
    /**
     * 產生ModelGetterSetter程式碼
     * @return
     */
    public static String genModelGetterSetter(String strColName, String strColType) {
        StringBuffer strbReturn = new StringBuffer();
        strbReturn.append("\n");
        
        //判斷使否為Blob欄位
        if(strColType.equals("BLOB") || strColType.equals("BINARY") || strColType.equals("VARBINARY")){
        	 //Byte format
            strbReturn.append("    private byte[] fl;\n");

            //Getter
            strbReturn.append("    public byte[] get" + strColName.substring(0,1).toUpperCase() + strColName.substring(1).toLowerCase() + "(){\n        return fl;\n    }\n");
            //Setter
            strbReturn.append("    public void set" + strColName.substring(0,1).toUpperCase() + strColName.substring(1).toLowerCase() + "(byte[] fl){\n        this.fl = fl;\n    }\n");

        }else{
            //Getter
            strbReturn.append("    public String get" + strColName.substring(0,1).toUpperCase() + strColName.substring(1).toLowerCase() + "(){\n        return getValue(\"" + strColName.toLowerCase() + "\");\n    }\n");
            //Setter
            strbReturn.append("    public void set" + strColName.substring(0,1).toUpperCase() + strColName.substring(1).toLowerCase() + "(String iVal){\n        setValue(\"" + strColName.toLowerCase() + "\", iVal);\n    }\n");
        }
    
        return strbReturn.toString();
    }
    
    /**
     * 產生ModelValidateGetterSetter程式碼
     * @return
     */
    public static String genModelValidateGetterSetter(String strColName, String strColType, String strColSize, String strColFraSize) {
        StringBuffer strbReturn = new StringBuffer();
        strbReturn.append("\n");
        
        //設定Validate Annotation
        //不產生檢核之欄位
        if(strColName.toLowerCase().indexOf("gftm") >= 0 || strColName.toLowerCase().indexOf("gfemp") >= 0 || strColName.toLowerCase().indexOf("txemp") >= 0 || strColName.toLowerCase().indexOf("txtm") >= 0 || strColName.toLowerCase().indexOf("fxuid") >= 0 || strColName.toLowerCase().indexOf("fl") >= 0 || strColType.equals("BLOB") || strColType.equals("BINARY") || strColType.equals("VARBINARY") || strColType.equals("CLOB")){
        	//do nothing
        }else{
        	strbReturn.append("    @NotEmpty\n");    
            //日期Validate
        	if(strColName.toLowerCase().indexOf("dat") >= 0 || strColName.toLowerCase().indexOf("date") >= 0 || strColName.toLowerCase().indexOf("tm") >= 0){
        		strbReturn.append("    @Future\n"); 
        		if(Integer.valueOf(strColSize) == 8){
        			strbReturn.append("    @DateTimeFormat(pattern=\"yyyy/MM/dd\")\n"); 
        		}
        	}
        	//EMAILValidate
        	if(strColName.toLowerCase().indexOf("mail") >= 0){
        		strbReturn.append("    @Email\n"); 
        	}
        	
        	
            if(strColType.equals("NUMERIC") || strColType.equals("DECIMAL")){
            	//數值Validate
            	 strbReturn.append("    @Min(value = 0)\n"); 
            	 strbReturn.append("    @Digits (integer = " + strColSize + ", fraction = " +  strColFraSize + ")\n"); 
            }else if(strColType.equals("DATE") || strColType.equals("NVARCHAR") || strColType.equals("VARCHAR")){
            	 //字串Validate
            	 strbReturn.append("    @Length(max = " + strColSize + ")\n"); 
            }
        	
        	 //Getter
            strbReturn.append("    public String getDc_" + strColName.toLowerCase() + "_0(){\n        return getValue(\"" + strColName.toLowerCase() + "\");\n    }\n");
            //Setter
            strbReturn.append("    public void setDc_" + strColName.toLowerCase()  + "_0(String iVal){\n        setValue(\"" + strColName.toLowerCase() + "\", iVal);\n    }\n");
        }
        
        return strbReturn.toString();
    }
    
    /**
     * 產生附屬清單資料宣告程式碼
     * @return
     */
    public static String genDeatailListDef(ArrayList<String> alTableDetail, String tableName, Hashtable<String, String> hstTableByDetialObject) {
        StringBuffer strbReturn = new StringBuffer();
    	for(int i=0; i<alTableDetail.size(); i++){
    		 String strDetailObjName = alTableDetail.get(i).toString();
    		 strbReturn.append("//<<" + strDetailObjName + ">>" + (String)hstTableByDetialObject.get(strDetailObjName) + "清單");
    		 strbReturn.append("\n");
    		 String objListDefName = strDetailObjName + "List";
    		 String objListName = strDetailObjName.substring(0,1).toLowerCase() + strDetailObjName.substring(1) + "List";
    	     strbReturn.append("    private " + objListDefName + " " + objListName + " = new " + objListDefName + "();\n\n");
             //Getter
             strbReturn.append("    public " + objListDefName + " get" + objListDefName + "(){\n        return " + objListName + ";\n    }\n");
             //Setter
             strbReturn.append("    public void set" + objListDefName + "(" + objListDefName + " " + objListName + "){\n        this." + objListName + " = " + objListName + ";\n    }\n\n");

    	}
       
        return strbReturn.toString();
    }
    
    /**
     * 產生附屬清單資料Import程式碼
     * @return
     */
    public static String genDeatailListImportData(ArrayList<String> alTableDetail, String packageName) {
        StringBuffer strbReturn = new StringBuffer();
    	for(int i=0; i<alTableDetail.size(); i++){
    		 String strDetailObjName = alTableDetail.get(i).toString();

    		 String objListImportDef = packageName + ".model.list." + strDetailObjName + "List";
    	     strbReturn.append("import " + objListImportDef + ";");
       	}
       
        return strbReturn.toString();
    }
    
    /**
     * 產生Condition日期區間查詢資料
     * @return
     */
    public static String genSearhDateField(ArrayList<String> alTableColumnName, String tableName) {
        StringBuffer strbReturn = new StringBuffer();
    	for(int i=0; i<alTableColumnName.size(); i++){
    		 String strColName = alTableColumnName.get(i).toString();
    		 
    		 //日期欄位才產生
    		 if(strColName.toLowerCase().indexOf("dat") >= 0 || strColName.toLowerCase().indexOf("date") >= 0 || strColName.toLowerCase().indexOf("tm") >= 0){
    			 String startDatColName = strColName.substring(0,1).toUpperCase() + strColName.substring(1).toLowerCase() + "StartDate";
    			 String endDatColName = strColName.substring(0,1).toUpperCase() + strColName.substring(1).toLowerCase() + "EndDate";
    			 
    			 strbReturn.append("\n");
    	         //StartDat Getter
    	         strbReturn.append("    public String get" + startDatColName + "(){\n        return getValue(\"" + startDatColName.toLowerCase() + "\");\n    }\n");
    	         //StartDat Setter
    	         strbReturn.append("    public void set" + startDatColName + "(String iVal){\n        setValue(\"" + startDatColName.toLowerCase() + "\", iVal);\n    }\n");

    	         //EndDat Getter
    	         strbReturn.append("    public String get" + endDatColName + "(){\n        return getValue(\"" + endDatColName.toLowerCase() + "\");\n    }\n");
    	         //EndDat Setter
    	         strbReturn.append("    public void set" + endDatColName + "(String iVal){\n        setValue(\"" + endDatColName.toLowerCase() + "\", iVal);\n    }\n");
    		 }
    	}
       
        return strbReturn.toString();
    }
    
    /**
     * 產生Condition查詢陣列條件查詢資料
     * @return
     */
    public static String genSearhArrayField(ArrayList<String> alTableColumnName, String tableName) {
        StringBuffer strbReturn = new StringBuffer();
    	for(int i=0; i<alTableColumnName.size(); i++){
    		 String strColName = alTableColumnName.get(i).toString();
    		 
    		 //狀態欄位產生
    		 if((strColName.toLowerCase().indexOf("tm") < 0 && strColName.toLowerCase().indexOf("xuid") < 0) && (strColName.toLowerCase().indexOf("sts") >= 0 || strColName.toLowerCase().indexOf("kd") >= 0 || strColName.toLowerCase().indexOf("type") >= 0)){
    			 String searhArrayColName = "Srh" + strColName.toLowerCase() + "Array";
    			 
        		 strbReturn.append("\n");
        	     strbReturn.append("    private ArrayList<String> " + searhArrayColName + " = new ArrayList<String>();\n");
                 //Getter
                 strbReturn.append("    public ArrayList<String> get" + searhArrayColName + "(){\n        return " + searhArrayColName + ";\n    }\n");
                 //Setter
                 strbReturn.append("    public void set" + searhArrayColName + "(ArrayList<String> " + searhArrayColName + "){\n        this." + searhArrayColName + " = " + searhArrayColName + ";\n    }\n");
    		 }
    	}
       
        return strbReturn.toString();
    }
    
    /**
     * 產生Condition排序欄位資料
     * @return
     */
    public static String genOrderByField(ArrayList<String> alTableColumnName, String tableName) {
        StringBuffer strbReturn = new StringBuffer();
    	for(int i=0; i<alTableColumnName.size(); i++){
    		 String strColName = alTableColumnName.get(i).toString();
    		 
    		 //排序欄位產生(僅產生最多前十個)
    		 if(i < 10){
    	    	 strbReturn.append("\n");
            	 strbReturn.append("    public static final String ORDERFIELD_" + strColName + "_Desc = \"" + strColName + " Desc\";//遞減排序\n");
            	 strbReturn.append("    public static final String ORDERFIELD_" + strColName + "_Asc = \"" + strColName + " Asc\";//遞增排序\n");
    		 }else{
    			 break;
    		 }
      	}
       
        return strbReturn.toString();
    }
    
    /**
     * 產生Condition自訂欄位資料
     * @return
     */
    public static String genCusSearhField(ArrayList<String> alTableColumnName, String tableName) {
        StringBuffer strbReturn = new StringBuffer();
    	for(int i=0; i<alTableColumnName.size(); i++){
    		 String strColName = alTableColumnName.get(i).toString();
    		 strColName = strColName.substring(0, 1).toUpperCase() + strColName.substring(1).toLowerCase();
    		 //自訂欄位資料產生(僅產生最多前十個)
    		 if(i < 10){
    			 String srhLikeColName = strColName + "LikeSrh";
    			 String srhLikeIgnCaseColName = strColName + "LikeIgnCaseSrh";
    			 String srhIgnCaseColName = strColName + "IgnCaseSrh";
    			 String srhIsNotEmptyColName = strColName + "IsNotEmptySrh";
    			 String srhIsEmptyColName = strColName + "IsEmptySrh";
    			 
        		 if(strColName.toLowerCase().indexOf("fxuid") < 0 && strColName.toLowerCase().indexOf("fl") < 0 && strColName.toLowerCase().indexOf("kd") < 0 && strColName.toLowerCase().indexOf("type") < 0 && strColName.toLowerCase().indexOf("sts") < 0 && strColName.toLowerCase().indexOf("dat") < 0 && strColName.toLowerCase().indexOf("date") < 0 && strColName.toLowerCase().indexOf("tm") < 0 && strColName.toLowerCase().indexOf("xuid") < 0){
        			 strbReturn.append("\n");
        	         //Like Getter
        	         strbReturn.append("    public String get" +srhLikeColName + "(){\n        return getValue(\"" + srhLikeColName.toLowerCase() + "\");\n    }\n");
        	         //Like Setter
        	         strbReturn.append("    public void set" + srhLikeColName + "(String iVal){\n        setValue(\"" + srhLikeColName.toLowerCase() + "\", iVal);\n    }\n");
        	         
        	         //LikeIgnCase Getter
        	         strbReturn.append("    public String get" +srhLikeIgnCaseColName + "(){\n        return getValue(\"" + srhLikeIgnCaseColName.toLowerCase() + "\");\n    }\n");
        	         //LikeIgnCase Setter
        	         strbReturn.append("    public void set" + srhLikeIgnCaseColName + "(String iVal){\n        setValue(\"" + srhLikeIgnCaseColName.toLowerCase() + "\", iVal);\n    }\n");
        	         
        	         //IgnCase Getter
        	         strbReturn.append("    public String get" +srhIgnCaseColName + "(){\n        return getValue(\"" + srhIgnCaseColName.toLowerCase() + "\");\n    }\n");
        	         //IgnCase Setter
        	         strbReturn.append("    public void set" + srhIgnCaseColName + "(String iVal){\n        setValue(\"" + srhIgnCaseColName.toLowerCase() + "\", iVal);\n    }\n");
        	         
        	         //IsNotEmpty Getter
        	         strbReturn.append("    public String get" +srhIsNotEmptyColName + "(){\n        return getValue(\"" + srhIsNotEmptyColName.toLowerCase() + "\");\n    }\n");
        	         //IsNotEmpty Setter
        	         strbReturn.append("    public void set" + srhIsNotEmptyColName + "(String iVal){\n        setValue(\"" + srhIsNotEmptyColName.toLowerCase() + "\", iVal);\n    }\n");

        	         
        	         //IsEmpty Getter
        	         strbReturn.append("    public String get" +srhIsEmptyColName + "(){\n        return getValue(\"" + srhIsEmptyColName.toLowerCase() + "\");\n    }\n");
        	         //IsEmpty Setter
        	         strbReturn.append("    public void set" + srhIsEmptyColName + "(String iVal){\n        setValue(\"" + srhIsEmptyColName.toLowerCase() + "\", iVal);\n    }\n");

        		 }	 
    		 }else{
    			 break;
    		 }
    	}
       
        return strbReturn.toString();
    }
   
    /**
	* 產生Model Validate
	*/
	public static void generateModelValidate(String theTableName, String theObjectName, ArrayList<String> alTableColumnName, ArrayList<String> alTableColumnType, ArrayList<String> alTableColumnSize, ArrayList<String> alTableColumnFraSize) throws Exception {
		//建立Package路徑
        File filePath = new File("./src/" + codeGenConfig.getObject().getPackage().replaceAll("\\.", "\\/") + "/model/validate/");
		if(!filePath.exists()){
			filePath.mkdirs();
		}

		//建立Model Validate物件
	    if(alTableColumnName != null && alTableColumnName.size() > 0 && alTableColumnType != null && alTableColumnType.size() > 0){
	    	String context = readTemplateFile("ModelValidateTemplate.txt");
	        StringBuffer contextSbr = new StringBuffer();
            String ObjectFileName = "./src/" + codeGenConfig.getObject().getPackage().replaceAll("\\.", "\\/").toLowerCase() + "/model/validate/" + theObjectName + "Validate.java";
            if(!"".equals(context)){
            	context = context.replaceAll("<<Author>>", codeGenConfig.getObject().getAuthor());	
            	context = context.replaceAll("<<PackageName>>", codeGenConfig.getObject().getPackage().toLowerCase());	
            	context = context.replaceAll("<<ObjectName>>", theObjectName);
            	context = context.replaceAll("<<TableName>>", theTableName);
            	for(int i=0; i<alTableColumnName.size(); i++){
            		contextSbr.append(genModelValidateGetterSetter(alTableColumnName.get(i).toString(), alTableColumnType.get(i).toString(), alTableColumnSize.get(i).toString(), alTableColumnFraSize.get(i).toString()));
            	}
            	
            	context = context.replaceAll("<<GetSetMethod>>", contextSbr.toString());
	        }
	            
	        FileWriter fileOut = new FileWriter(ObjectFileName, false);
	        fileOut.write(context);
	        fileOut.flush();
	        fileOut.close();
	    }
	    
	}
	
	/**
	* 產生BO Interface
	*/
	public static void generateBOInterface(String strModuleName, Hashtable<String, String> hstTableByObjName) throws Exception {
		//建立Package路徑
        File filePath = new File("./src/" + codeGenConfig.getObject().getPackage().replaceAll("\\.", "\\/") + "/bo/");
		if(!filePath.exists()){
			filePath.mkdirs();
		}
	       
		//取得物件TABLE資料，以產生BO Method
		Set<String> set = hstTableByObjName.keySet();
		Iterator<String> itr = set.iterator();
        String BOMethodContext = "";
        StringBuffer BOMethodContextSbr = new StringBuffer();
        
		while(itr.hasNext()){
			String key = itr.next();
			String theTableName = key;
			String theObjectName = (String)hstTableByObjName.get(key);
			
			BOMethodContext = "";
			BOMethodContext = readTemplateFile("BOMethodTemplate.txt");
            if(!"".equals(BOMethodContext)){
            	BOMethodContext = BOMethodContext.replaceAll("<<ObjectName>>", theObjectName);
            	BOMethodContext = BOMethodContext.replaceAll("<<TableName>>", theTableName);
              	
            	BOMethodContextSbr.append(BOMethodContext);
            }
			
		}
			
		//建立BO Interface物件
    	String context = readTemplateFile("BOTemplate.txt");
        String ObjectFileName = "./src/" + codeGenConfig.getObject().getPackage().replaceAll("\\.", "\\/").toLowerCase() + "/bo/" + strModuleName + "BO.java";
        
        if(!"".equals(context)){
        	context = context.replaceAll("<<Author>>", codeGenConfig.getObject().getAuthor());	
        	context = context.replaceAll("<<PackageName>>", codeGenConfig.getObject().getPackage().toLowerCase());	
        	context = context.replaceAll("<<ModuleName>>", strModuleName);
        	context = context.replaceAll("<<BOMethod>>", BOMethodContextSbr.toString());
        }
            
       
        FileWriter fileOut = new FileWriter(ObjectFileName, false);
        fileOut.write(context);
        fileOut.flush();
        fileOut.close();
	    
	}
	
	/**
	* 產生BO Implement Interface
	*/
	public static void generateBOImplement(String strModuleName, Hashtable<String, String> hstTableByObjName, Hashtable<String, ArrayList<String>> hstTableByDetial, Hashtable<String, String> hstTableByDetialObject) throws Exception {
		//建立Package路徑
        File filePath = new File("./src/" + codeGenConfig.getObject().getPackage().replaceAll("\\.", "\\/") + "/bo/");
		if(!filePath.exists()){
			filePath.mkdirs();
		}
	       
		//取得物件TABLE資料，以產生BO Implement Method
		Set<String> set = hstTableByObjName.keySet();
		Iterator<String> itr = set.iterator();
        String BOImplMethodContext = "";
        StringBuffer BOImplMethodContextSbr = new StringBuffer();
        
		while(itr.hasNext()){
			String key = itr.next();
			String theTableName = key;
			String theObjectName = (String)hstTableByObjName.get(key);
			ArrayList<String> alTableDetial = (ArrayList<String>)hstTableByDetial.get(key);//Table 附屬資料名稱
			
			BOImplMethodContext = "";
			BOImplMethodContext = readTemplateFile("BOImplMethodTemplate.txt");
            if(!"".equals(BOImplMethodContext)){
            	BOImplMethodContext = BOImplMethodContext.replaceAll("<<ObjectName>>", theObjectName);
            	BOImplMethodContext = BOImplMethodContext.replaceAll("<<ObjectNameFrLowCase>>", theObjectName.substring(0,1).toLowerCase() + theObjectName.substring(1));
            	BOImplMethodContext = BOImplMethodContext.replaceAll("<<ModuleName>>",  strModuleName);
            	BOImplMethodContext = BOImplMethodContext.replaceAll("<<ModuleNameFrLowCase>>",  strModuleName.substring(0,1).toLowerCase() + strModuleName.substring(1));
            	BOImplMethodContext = BOImplMethodContext.replaceAll("<<TableName>>", theTableName);
            	BOImplMethodContext = BOImplMethodContext.replaceAll("<<InculdeDetailMethod>>", genInculdeDetailMethodCode(alTableDetial, hstTableByDetialObject));
            	BOImplMethodContextSbr.append(BOImplMethodContext);
            }
 		}
			
		//建立BO Implement物件
    	String context = readTemplateFile("BOImplTemplate.txt");
        String ObjectFileName = "./src/" + codeGenConfig.getObject().getPackage().replaceAll("\\.", "\\/").toLowerCase() + "/bo/" + strModuleName + "BOImpl.java";
        
        if(!"".equals(context)){
        	context = context.replaceAll("<<Author>>", codeGenConfig.getObject().getAuthor());	
        	context = context.replaceAll("<<PackageName>>", codeGenConfig.getObject().getPackage().toLowerCase());	
        	context = context.replaceAll("<<ModuleName>>", strModuleName);
        	context = context.replaceAll("<<ModuleNameFrLowCase>>", strModuleName.substring(0,1).toLowerCase() + strModuleName.substring(1));
        	context = context.replaceAll("<<DAOAutowiredInit>>", genDAOAutowiredInitCode(hstTableByObjName));
        	if(hstTableByDetial == null || hstTableByDetial.isEmpty()){
        		context = context.replaceAll("<<InculdeDetail>>", "public static int INCLUDE_ALL = 1023;\n");
        	}else{
        		context = context.replaceAll("<<InculdeDetail>>", genInculdeDetailIndexCode(hstTableByDetial, hstTableByDetialObject));
        	}
        	context = context.replaceAll("<<BOImplMethod>>", BOImplMethodContextSbr.toString());
        }
            
        FileWriter fileOut = new FileWriter(ObjectFileName, false);
        fileOut.write(context);
        fileOut.flush();
        fileOut.close();
	    
	}
	
	/**
	* 產生UtilBO Interface
	*/
	public static void generateUtilBOInterface(String strModuleName, Hashtable<String, String> hstTableByObjName) throws Exception {
		//建立Package路徑
        File filePath = new File("./src/" + codeGenConfig.getObject().getPackage().replaceAll("\\.", "\\/") + "/bo/");
		if(!filePath.exists()){
			filePath.mkdirs();
		}
	       
		//取得物件TABLE資料，以產生BO Method
		Set<String> set = hstTableByObjName.keySet();
		Iterator<String> itr = set.iterator();
        String BOMethodContext = "";
        StringBuffer BOMethodContextSbr = new StringBuffer();
        
		while(itr.hasNext()){
			String key = itr.next();
			String theTableName = key;
			String theObjectName = (String)hstTableByObjName.get(key);
			
			BOMethodContext = "";
			BOMethodContext = readTemplateFile("UtilBOMethodTemplate.txt");
            if(!"".equals(BOMethodContext)){
            	BOMethodContext = BOMethodContext.replaceAll("<<ObjectName>>", theObjectName);
            	BOMethodContext = BOMethodContext.replaceAll("<<TableName>>", theTableName);
              	
            	BOMethodContextSbr.append(BOMethodContext);
            }
			
		}
			
		//建立BO Interface物件
    	String context = readTemplateFile("UtilBOTemplate.txt");
        String ObjectFileName = "./src/" + codeGenConfig.getObject().getPackage().replaceAll("\\.", "\\/").toLowerCase() + "/bo/" + strModuleName + "UtilBO.java";
        
        if(!"".equals(context)){
        	context = context.replaceAll("<<Author>>", codeGenConfig.getObject().getAuthor());	
        	context = context.replaceAll("<<PackageName>>", codeGenConfig.getObject().getPackage().toLowerCase());	
        	context = context.replaceAll("<<ModuleName>>", strModuleName);
        	context = context.replaceAll("<<BOMethod>>", BOMethodContextSbr.toString());
        }
            
        FileWriter fileOut = new FileWriter(ObjectFileName, false);
        fileOut.write(context);
        fileOut.flush();
        fileOut.close();
	    
	}
	
	/**
	* 產生UtilBO Implement Interface
	*/
	public static void generateUtilBOImplement(String strModuleName, Hashtable<String, String> hstTableByObjName) throws Exception {
		//建立Package路徑
        File filePath = new File("./src/" + codeGenConfig.getObject().getPackage().replaceAll("\\.", "\\/") + "/bo/");
		if(!filePath.exists()){
			filePath.mkdirs();
		}
	       
		//取得物件TABLE資料，以產生BO Implement Method
		Set<String> set = hstTableByObjName.keySet();
		Iterator<String> itr = set.iterator();
        String BOImplMethodContext = "";
        StringBuffer BOImplMethodContextSbr = new StringBuffer();
        
		while(itr.hasNext()){
			String key = itr.next();
			String theTableName = key;
			String theObjectName = (String)hstTableByObjName.get(key);
			
			BOImplMethodContext = "";
			BOImplMethodContext = readTemplateFile("UtilBOImplMethodTemplate.txt");
            if(!"".equals(BOImplMethodContext)){
            	BOImplMethodContext = BOImplMethodContext.replaceAll("<<ObjectName>>", theObjectName);
            	BOImplMethodContext = BOImplMethodContext.replaceAll("<<ObjectNameFrLowCase>>", theObjectName.substring(0,1).toLowerCase() + theObjectName.substring(1));
            	BOImplMethodContext = BOImplMethodContext.replaceAll("<<ModuleName>>",  strModuleName);
            	BOImplMethodContext = BOImplMethodContext.replaceAll("<<ModuleNameFrLowCase>>",  strModuleName.substring(0,1).toLowerCase() + strModuleName.substring(1));
            	BOImplMethodContext = BOImplMethodContext.replaceAll("<<TableName>>", theTableName);
            	BOImplMethodContextSbr.append(BOImplMethodContext);
            }
			
		}
			
		//建立BO Implement物件
    	String context = readTemplateFile("UtilBOImplTemplate.txt");
        String ObjectFileName = "./src/" + codeGenConfig.getObject().getPackage().replaceAll("\\.", "\\/").toLowerCase() + "/bo/" + strModuleName + "UtilBOImpl.java";
        
        if(!"".equals(context)){
        	context = context.replaceAll("<<Author>>", codeGenConfig.getObject().getAuthor());	
        	context = context.replaceAll("<<PackageName>>", codeGenConfig.getObject().getPackage().toLowerCase());	
        	context = context.replaceAll("<<ModuleName>>", strModuleName);
        	context = context.replaceAll("<<ModuleNameFrLowCase>>", strModuleName.substring(0,1).toLowerCase() + strModuleName.substring(1));
        	context = context.replaceAll("<<DAOAutowiredInit>>", genDAOAutowiredInitCode(hstTableByObjName));
        	context = context.replaceAll("<<BOImplMethod>>", BOImplMethodContextSbr.toString());
        }
            
        FileWriter fileOut = new FileWriter(ObjectFileName, false);
        fileOut.write(context);
        fileOut.flush();
        fileOut.close();
	    
	}
	
	/**
     * 產生DAOAutowiredInit程式碼
     * @return
     */
    public static String genDAOAutowiredInitCode(Hashtable<String, String> hstTableByObjName) throws Exception  {
        StringBuffer strbReturn = new StringBuffer();
        
		//取得物件TABLE資料
		Set<String> set = hstTableByObjName.keySet();
		Iterator<String> itr = set.iterator();
		while(itr.hasNext()){
			String key = itr.next();//Table Name
			String theObjectName = (String)hstTableByObjName.get(key);
			strbReturn.append("\n");
			strbReturn.append("    @Autowired\n");
			strbReturn.append("    private " + theObjectName + "DAO " + theObjectName.substring(0,1).toLowerCase() + theObjectName.substring(1) + "DAO;\n");
		}	

        return strbReturn.toString();
    }
    
	/**
     * 產生InculdeDetail Index程式碼
     * @return
     */
    public static String genInculdeDetailIndexCode(Hashtable<String, ArrayList<String>> hstTableByDetial, Hashtable<String, String> hstTableByDetialObject) throws Exception  {
        StringBuffer strbReturn = new StringBuffer();
        int index = 2;
		//取得附屬明細物件TABLE資料
		Set<String> set = hstTableByDetial.keySet();
		Iterator<String> itr = set.iterator();
		while(itr.hasNext()){
			String key = itr.next();//Table Name
			ArrayList<String> alTableDetial = (ArrayList<String>)hstTableByDetial.get(key);//Table 附屬資料名稱
	      	if(alTableDetial != null && alTableDetial.size() > 0){
	      		
	      		strbReturn.append("\n");
	      		for(int i=0; i<alTableDetial.size(); i++){
	      			String strDetailName = (String)alTableDetial.get(i);
	      			strbReturn.append("    private static int INCLUDE_" + strDetailName + " = " + index + ";//(" + (String)hstTableByDetialObject.get(strDetailName) + ")\n");
	      			index = index * 2;
	      		}
	      		
	      		
        	}
		}	

		strbReturn.append("    public static int INCLUDE_ALL = " + (--index) + ";\n");
		
        return strbReturn.toString();
    }
    
    /**
     * 產生InculdeDetail Method程式碼
     * @return
     */
    public static String genInculdeDetailMethodCode(ArrayList<String> alTableDetial, Hashtable<String, String> hstTableByDetialObject) throws Exception  {
        String inculdeDetailMethodContext = "";
        StringBuffer inculdeDetailMethodContextSbr = new StringBuffer();
			
		if(alTableDetial != null && alTableDetial.size() > 0){
			inculdeDetailMethodContextSbr.append("\n");
			for(int i=0; i<alTableDetial.size(); i++){
				String strDetailObjectName = (String)alTableDetial.get(i);
				inculdeDetailMethodContext = "";
				inculdeDetailMethodContext = readTemplateFile("BOImplIncludeMethodTemplate.txt");
	            if(!"".equals(inculdeDetailMethodContext)){
	            	inculdeDetailMethodContext = inculdeDetailMethodContext.replaceAll("<<DetailObjectName>>", strDetailObjectName);
	            	inculdeDetailMethodContext = inculdeDetailMethodContext.replaceAll("<<DetailObjectNameFrLowCase>>", strDetailObjectName.substring(0,1).toLowerCase() + strDetailObjectName.substring(1));
	            	inculdeDetailMethodContext = inculdeDetailMethodContext.replaceAll("<<TableName>>", (String)hstTableByDetialObject.get(strDetailObjectName));
	            	inculdeDetailMethodContextSbr.append(inculdeDetailMethodContext);
	            	inculdeDetailMethodContextSbr.append("\n");
	            }
			}
		}
	
		return inculdeDetailMethodContextSbr.toString();
    }
    
    /**
	* 產生DAO Interface
	*/
	public static void generateDAOInterface(String theTableName, String theObjectName) throws Exception {
		//建立Package路徑
        File filePath = new File("./src/" + codeGenConfig.getObject().getPackage().replaceAll("\\.", "\\/") + "/dao/");
		if(!filePath.exists()){
			filePath.mkdirs();
		}

		//建立DAO Interface物件
    	String context = readTemplateFile("DAOTemplate.txt");
        String ObjectFileName = "./src/" + codeGenConfig.getObject().getPackage().replaceAll("\\.", "\\/").toLowerCase() + "/dao/" + theObjectName + "DAO.java";
        if(!"".equals(context)){
        	context = context.replaceAll("<<Author>>", codeGenConfig.getObject().getAuthor());	
        	context = context.replaceAll("<<PackageName>>", codeGenConfig.getObject().getPackage().toLowerCase());	
        	context = context.replaceAll("<<ObjectName>>", theObjectName);
        	context = context.replaceAll("<<TableName>>", theTableName);
        }
            
        FileWriter fileOut = new FileWriter(ObjectFileName, false);
        fileOut.write(context);
        fileOut.flush();
        fileOut.close();
	}
	
	/**
	* 產生UtilDAO Interface
	*/
	public static void generateUtilDAOInterface(String strModuleName, Hashtable<String, String> hstTableByObjName) throws Exception {
		//建立Package路徑
        File filePath = new File("./src/" + codeGenConfig.getObject().getPackage().replaceAll("\\.", "\\/") + "/dao/");
		if(!filePath.exists()){
			filePath.mkdirs();
		}
	       
		//取得物件資料，以產生UtilDAO Method
		Set<String> set = hstTableByObjName.keySet();
		Iterator<String> itr = set.iterator();
        String UtilDAOMethodContext = "";
        StringBuffer UtilDAOMethodContextSbr = new StringBuffer();
        UtilDAOMethodContextSbr.append("\n");
        
		while(itr.hasNext()){
			String key = itr.next();
			String theTableName = key;
			String theObjectName = (String)hstTableByObjName.get(key);
			
			UtilDAOMethodContext = "";
			UtilDAOMethodContext = readTemplateFile("UtilDAOMethodTemplate.txt");
            if(!"".equals(UtilDAOMethodContext)){
            	UtilDAOMethodContext = UtilDAOMethodContext.replaceAll("<<ObjectName>>", theObjectName);
            	UtilDAOMethodContext = UtilDAOMethodContext.replaceAll("<<TableName>>", theTableName);
              	
            	UtilDAOMethodContextSbr.append(UtilDAOMethodContext);
            }
			
		}
			
		//建立UtilDAO Interface物件
    	String context = readTemplateFile("UtilDAOTemplate.txt");
        String ObjectFileName = "./src/" + codeGenConfig.getObject().getPackage().replaceAll("\\.", "\\/").toLowerCase() + "/dao/" + strModuleName + "UtilDAO.java";
        
        if(!"".equals(context)){
        	context = context.replaceAll("<<Author>>", codeGenConfig.getObject().getAuthor());	
        	context = context.replaceAll("<<PackageName>>", codeGenConfig.getObject().getPackage().toLowerCase());	
        	context = context.replaceAll("<<ModuleName>>", strModuleName);
        	context = context.replaceAll("<<UtilDAOMethod>>", UtilDAOMethodContextSbr.toString());
        }
            
        FileWriter fileOut = new FileWriter(ObjectFileName, false);
        fileOut.write(context);
        fileOut.flush();
        fileOut.close();
	    
	}
	
	/**
	* 產生DAO Mapper
	*/
	public static void generateDAOMapper(String theTableName, String theObjectName, String strDbType, ArrayList<String> alTableColumnName, ArrayList<String> alTableColumnType) throws Exception {
		//建立Package路徑
        File filePath = new File("./src/" + codeGenConfig.getObject().getPackage().replaceAll("\\.", "\\/") + "/dao/");
		if(!filePath.exists()){
			filePath.mkdirs();
		}
		
        filePath = new File("./src/" + codeGenConfig.getObject().getPackage().replaceAll("\\.", "\\/") + "/dao/xml/");
		if(!filePath.exists()){
			filePath.mkdirs();
		}

		//建立DAO物件
	    if(alTableColumnName != null && alTableColumnName.size() > 0 && alTableColumnType != null && alTableColumnType.size() > 0){
	    	String context = readTemplateFile("DAOMapperTemplate.txt");
	        StringBuffer contextSbr = new StringBuffer();
            String ObjectFileName = "./src/" + codeGenConfig.getObject().getPackage().replaceAll("\\.", "\\/").toLowerCase() + "/dao/xml/" + theObjectName + "DAO.xml";
            if(!"".equals(context)){
            	context = context.replaceAll("<<Author>>", codeGenConfig.getObject().getAuthor());	
            	context = context.replaceAll("<<PackageName>>", codeGenConfig.getObject().getPackage().toLowerCase());	
            	context = context.replaceAll("<<ObjectName>>", theObjectName);
            	if(codeGenConfig.getObject().getCreateTableProperty().equals("Y")){
            		context = context.replaceAll("<<TableName>>", "\\${" + theObjectName + "}");
            	}else{
            		context = context.replaceAll("<<TableName>>", theTableName);
            	}
            	context = context.replaceAll("<<BaseResultMap>>", genBaseResultMap(alTableColumnName, alTableColumnType));
            	context = context.replaceAll("<<BaseCol>>", genBaseCol(alTableColumnName, alTableColumnType));
             	context = context.replaceAll("<<InsertColVal>>", genInsertColVal(alTableColumnName, alTableColumnType));
            	context = context.replaceAll("<<InsertSelectiveCol>>", genInsertSelectiveCol(alTableColumnName, alTableColumnType));
            	context = context.replaceAll("<<InsertSelectiveColVal>>", genInsertSelectiveColVal(alTableColumnName, alTableColumnType));
            	context = context.replaceAll("<<SelectListBySelectiveColVal>>", genSelectListBySelectiveColVal(alTableColumnName, alTableColumnType));
            	context = context.replaceAll("<<UpdateByPrimaryKeyColVal>>", genUpdateByPrimaryKeyColVal(alTableColumnName, alTableColumnType));
            	context = context.replaceAll("<<UpdateByPrimaryKeySelectiveColVal>>", genUpdateByPrimaryKeySelectiveColVal(alTableColumnName, alTableColumnType));
            	if(isHasDataField){
            		context = context.replaceAll("<<BaseResultMapWithData>>", genBaseResultMapWithData(alTableColumnName, alTableColumnType));
                	context = context.replaceAll("<<BaseColWithoutData>>", genBaseColWithoutData(alTableColumnName, alTableColumnType));
            		context = context.replaceAll("<<SelByPrimaryKeyBaseResultMapDef>>", "BaseResultMapWithData");
            		context = context.replaceAll("<<SelListBySelectiveBaseColDef>>", "BaseColWithoutData");
            	}else{
            		context = context.replaceAll("<<BaseResultMapWithData>>", "");
                	context = context.replaceAll("<<BaseColWithoutData>>", "");
            		context = context.replaceAll("<<SelByPrimaryKeyBaseResultMapDef>>", "BaseResultMap");
            		context = context.replaceAll("<<SelListBySelectiveBaseColDef>>", "BaseCol");
            	}
            	
            	for(int i=0; i<alTableColumnName.size(); i++){
            		contextSbr.append(genModelGetterSetter(alTableColumnName.get(i).toString(), alTableColumnType.get(i).toString()));
            	}
            	
            	
	        }
	            
	        FileWriter fileOut = new FileWriter(ObjectFileName, false);
	        fileOut.write(context);
	        fileOut.flush();
	        fileOut.close();
	    }
	    
	}
	
	
	/**
     * 產生BaseResultMap程式碼
     * @return
     */
    public static String genBaseResultMap(ArrayList<String> alTableColumnName, ArrayList<String> alTableColumnType) {
        StringBuffer strbReturn = new StringBuffer();
        strbReturn.append("\n");
        
        for(int i=0; i<alTableColumnName.size(); i++){
        	String strColName = (String)alTableColumnName.get(i).toString();
        	String strColNameLowCase = strColName.toLowerCase();
        	String strColType = (String)alTableColumnType.get(i).toString();
        	
        	if(i == 0){
        		strbReturn.append("      <id column=\"" + strColName + "\" property=\"" + strColNameLowCase + "\" jdbcType=\"" + strColType + "\" />\n");
        	}else if(!strColType.equals("BLOB") && !strColType.equals("BINARY") && !strColType.equals("VARBINARY") && !strColType.equals("CLOB")){
        		//判斷使否為Blob&Clob欄位
        		strbReturn.append("      <result column=\"" + strColName + "\" property=\"" + strColNameLowCase + "\" jdbcType=\"" + strColType + "\" />\n");
        	}
        	
        	if(strColType.equals("BLOB") || strColType.equals("BINARY") || strColType.equals("VARBINARY") || strColType.equals("CLOB")){
        		isHasDataField = true;
        	}
        }
        
        return strbReturn.toString();
    }
    
	/**
     * 產生BaseCol程式碼
     * @return
     */
    public static String genBaseCol(ArrayList<String> alTableColumnName, ArrayList<String> alTableColumnType) {
        StringBuffer strbReturn = new StringBuffer();
        strbReturn.append("  ");
        
        for(int i=0; i<alTableColumnName.size(); i++){
        	String strColName = (String)alTableColumnName.get(i).toString();
        	strbReturn.append(strColName);
        	if((i+1) < alTableColumnName.size()){
        		strbReturn.append(", ");
        	}
        	if((i>0) && (i % 15) == 0){
        		strbReturn.append("\n");
        		strbReturn.append("        ");
        	}
        }
        
        return strbReturn.toString();
    }
    
	/**
     * 產生BaseResultMapWithData程式碼(BLOB Type才使用)
     * @return
     */
    public static String genBaseResultMapWithData(ArrayList<String> alTableColumnName, ArrayList<String> alTableColumnType) {
        StringBuffer strbReturn = new StringBuffer();
        strbReturn.append("\n");
        
        for(int i=0; i<alTableColumnName.size(); i++){
        	String strColName = (String)alTableColumnName.get(i).toString();
        	String strColNameLowCase = strColName.toLowerCase();
        	String strColType = (String)alTableColumnType.get(i).toString();
        	
        	if(strColType.equals("BLOB") || strColType.equals("BINARY") || strColType.equals("VARBINARY") || strColType.equals("CLOB")){
        		//判斷使否為Blob欄位
        		strbReturn.append("      <result column=\"" + strColName + "\" property=\"" + strColNameLowCase + "\" jdbcType=\"" + strColType + "\" />\n");
        	}
        }

        return strbReturn.toString();
    }
    
    /**
     * 產生BaseColWithoutData程式碼
     * @return
     */
    public static String genBaseColWithoutData(ArrayList<String> alTableColumnName, ArrayList<String> alTableColumnType) {
        StringBuffer strbReturn = new StringBuffer();
        strbReturn.append("  ");
        
        for(int i=0; i<alTableColumnName.size(); i++){
        	String strColName = (String)alTableColumnName.get(i).toString();
        	String strColType = (String)alTableColumnType.get(i).toString();
    	   
        	if(!strColType.equals("BLOB") && !strColType.equals("BINARY") && !strColType.equals("VARBINARY") && !strColType.equals("CLOB")){
            	strbReturn.append(strColName);
            	if((i+1) < alTableColumnName.size()){
            		strbReturn.append(", ");
            	}
            	if((i>0) && (i % 15) == 0){
              		strbReturn.append("\n");
            		strbReturn.append("        ");
            	}
        	}
        }
        
        return strbReturn.toString();
    }
    
	/**
     * 產生InsertColVal程式碼
     * @return
     */
    public static String genInsertColVal(ArrayList<String> alTableColumnName, ArrayList<String> alTableColumnType) {
        StringBuffer strbReturn = new StringBuffer();
        
        for(int i=0; i<alTableColumnName.size(); i++){
        	String strColName = (String)alTableColumnName.get(i).toString();
        	String strColNameLowCase = strColName.toLowerCase();
        	String strColType = (String)alTableColumnType.get(i).toString();
        	
        	strbReturn.append("#{" + strColNameLowCase + ",jdbcType=" + strColType + "}");
        	if((i+1) < alTableColumnName.size()){
        		strbReturn.append(", ");
        	}
        }
        
        return strbReturn.toString();
    }
    
	/**
     * 產生InsertSelectiveCol程式碼
     * @return
     */
    public static String genInsertSelectiveCol(ArrayList<String> alTableColumnName, ArrayList<String> alTableColumnType) {
        StringBuffer strbReturn = new StringBuffer();
        strbReturn.append("\n");
        
        for(int i=0; i<alTableColumnName.size(); i++){
        	String strColName = (String)alTableColumnName.get(i).toString();
        	String strColNameLowCase = strColName.toLowerCase();
        	
        	strbReturn.append("         <if test=\"" + strColNameLowCase + " != null \">\n");
        	strbReturn.append("             " + strColName + ",\n");
        	strbReturn.append("         </if>\n");
        }
        
        return strbReturn.toString();
    }
    
	/**
     * 產生InsertSelectiveColVal程式碼
     * @return
     */
    public static String genInsertSelectiveColVal(ArrayList<String> alTableColumnName, ArrayList<String> alTableColumnType) {
        StringBuffer strbReturn = new StringBuffer();
        strbReturn.append("\n");
        
        for(int i=0; i<alTableColumnName.size(); i++){
        	String strColName = (String)alTableColumnName.get(i).toString();
        	String strColNameLowCase = strColName.toLowerCase();
        	String strColType = (String)alTableColumnType.get(i).toString();
        	
        	strbReturn.append("         <if test=\"" + strColNameLowCase + " != null \">\n");
        	strbReturn.append("             #{" + strColNameLowCase + ",jdbcType=" + strColType + "},\n");
        	strbReturn.append("         </if>\n");
        }
        
        return strbReturn.toString();
    }
    
	/**
     * 產生SelectListBySelectiveColVal程式碼
     * @return
     */
    public static String genSelectListBySelectiveColVal(ArrayList<String> alTableColumnName, ArrayList<String> alTableColumnType) {
        StringBuffer strbReturn = new StringBuffer();
        strbReturn.append("\n");
        
        for(int i=0; i<alTableColumnName.size(); i++){
        	String strColName = (String)alTableColumnName.get(i).toString();
        	String strColNameLowCase = strColName.toLowerCase();
        	String strColType = (String)alTableColumnType.get(i).toString();
        	
        	//Blob欄位不產生為條件
        	if(!strColName.toLowerCase().equals("fl")){
        	  	strbReturn.append("         <if test=\"" + strColNameLowCase + " != '' \">\n");
            	if(i == 0){
            		strbReturn.append("            " + strColName + " = #{" + strColNameLowCase + ",jdbcType=" + strColType + "}\n");
            	}else{
            		strbReturn.append("            AND " + strColName + " = #{" + strColNameLowCase + ",jdbcType=" + strColType + "}\n");
            	}
            	strbReturn.append("         </if>\n");
        	}
        }
        
        return strbReturn.toString();
    }
    
	/**
     * 產生UpdateByPrimaryKeyColVal程式碼
     * @return
     */
    public static String genUpdateByPrimaryKeyColVal(ArrayList<String> alTableColumnName, ArrayList<String> alTableColumnType) {
        StringBuffer strbReturn = new StringBuffer();
        strbReturn.append("\n");
        for(int i=1; i<alTableColumnName.size(); i++){
        	String strColName = (String)alTableColumnName.get(i).toString();
        	String strColNameLowCase = strColName.toLowerCase();
        	String strColType = (String)alTableColumnType.get(i).toString();
        	
    		strbReturn.append("         " + strColName + " = #{" + strColNameLowCase + ",jdbcType=" + strColType + "}");
        	if((i+1) < alTableColumnName.size()){
        		strbReturn.append(", ");
        	}
        	strbReturn.append("\n");
        }
        
        return strbReturn.toString();
    }
    
	/**
     * 產生UpdateByPrimaryKeySelectiveColVal程式碼
     * @return
     */
    public static String genUpdateByPrimaryKeySelectiveColVal(ArrayList<String> alTableColumnName, ArrayList<String> alTableColumnType) {
        StringBuffer strbReturn = new StringBuffer();
        strbReturn.append("\n");
        
        for(int i=1; i<alTableColumnName.size(); i++){
        	String strColName = (String)alTableColumnName.get(i).toString();
        	String strColNameLowCase = strColName.toLowerCase();
        	String strColType = (String)alTableColumnType.get(i).toString();
        	
        	strbReturn.append("         <if test=\"" + strColNameLowCase + " != null \">\n");
        	strbReturn.append("            " + strColName + " = #{" + strColNameLowCase + ",jdbcType=" + strColType + "},\n");
        	strbReturn.append("         </if>\n");
        }
        
        return strbReturn.toString();
    }
    
   /**
	* 產生UtilDAO Mapper
	*/
	public static void generateUtilDAOMapper(String strModuleName, String strDbType, Hashtable<String, String> hstTableByObjName, Hashtable<String, ArrayList<String>> hstTableByColName, Hashtable<String, ArrayList<String>> hstTableByColType) throws Exception {
		//建立Package路徑
	    File filePath = new File("./src/" + codeGenConfig.getObject().getPackage().replaceAll("\\.", "\\/") + "/dao/" + strDbType + "/");
		if(!filePath.exists()){
			filePath.mkdirs();
		}
		
        filePath = new File("./src/" + codeGenConfig.getObject().getPackage().replaceAll("\\.", "\\/") + "/dao/" + strDbType + "/xml/");
		if(!filePath.exists()){
			filePath.mkdirs();
		}
	       
		//取得物件資料，以產生UtilDAO Mapper Method
		Set<String> set = hstTableByObjName.keySet();
		Iterator<String> itr = set.iterator();
        String UtilDAOMapperContext = "";
        StringBuffer UtilDAOMapperContextSbr = new StringBuffer();
        UtilDAOMapperContextSbr.append("\n");
        
		while(itr.hasNext()){
			String key = itr.next();
			String theTableName = key;
			String theObjectName = (String)hstTableByObjName.get(key);
			ArrayList<String> alTableColumnName = (ArrayList<String>)hstTableByColName.get(key);//ColumnName名稱
			ArrayList<String> alTableColumnType = (ArrayList<String>)hstTableByColType.get(key);//ColumnType名稱
			
			UtilDAOMapperContext = "";
			if(codeGenConfig.getDbConnection().getType().equals("MSSQL")){
				UtilDAOMapperContext = readTemplateFile("UtilDAOMapperMethodMSSQLTemplate.txt");
			}else{
				UtilDAOMapperContext = readTemplateFile("UtilDAOMapperMethodORACLETemplate.txt");
			}
			
            if(!"".equals(UtilDAOMapperContext)){
            	UtilDAOMapperContext = UtilDAOMapperContext.replaceAll("<<ObjectName>>", theObjectName);
               	if(codeGenConfig.getObject().getCreateTableProperty().equals("Y")){
               		UtilDAOMapperContext = UtilDAOMapperContext.replaceAll("<<TableName>>", "\\\\\\${" + theObjectName + "}");
            	}else{
            		UtilDAOMapperContext = UtilDAOMapperContext.replaceAll("<<TableName>>", theTableName);
            	}
               	UtilDAOMapperContext = UtilDAOMapperContext.replaceAll("<<TableNameVal>>", theTableName);  
               	
        		StringBuffer TableGetFieldNameSbr = new StringBuffer();
                for(int i=0; i<alTableColumnName.size(); i++){
                	String strColName = (String)alTableColumnName.get(i).toString();
                	String strColType = (String)alTableColumnType.get(i).toString();
                	
                	if(!strColType.equals("BLOB") && !strColType.equals("BINARY") && !strColType.equals("VARBINARY") && !strColType.equals("CLOB")){
                		TableGetFieldNameSbr.append(strColName);
                    	if((i+1) < alTableColumnName.size()){
                    		TableGetFieldNameSbr.append(", ");
                    	}
                  	}
                }
        		UtilDAOMapperContext = UtilDAOMapperContext.replaceAll("<<TableGetFieldName>>", TableGetFieldNameSbr.toString());
            	
            	UtilDAOMapperContext = UtilDAOMapperContext.replaceAll("<<PackageName>>", codeGenConfig.getObject().getPackage().toLowerCase());
            	UtilDAOMapperContext = UtilDAOMapperContext.replaceAll("<<Conditions>>", generateUtilDAOMapperCondition(theTableName, theObjectName, alTableColumnName, alTableColumnType));
            	UtilDAOMapperContextSbr.append(UtilDAOMapperContext);
            }
			
		}
			
		//建立UtilDAO Mapper物件
    	String context = readTemplateFile("UtilDAOMapperTemplate.txt");
        String ObjectFileName = "./src/" + codeGenConfig.getObject().getPackage().replaceAll("\\.", "\\/").toLowerCase() + "/dao/" + strDbType + "/xml/" + strModuleName + "UtilDAO.xml";
        
        if(!"".equals(context)){
        	context = context.replaceAll("<<Author>>", codeGenConfig.getObject().getAuthor());	
        	context = context.replaceAll("<<PackageName>>", codeGenConfig.getObject().getPackage().toLowerCase());	
        	context = context.replaceAll("<<ModuleName>>", strModuleName);

        	context = context.replaceAll("<<UtilDAOMapperMethod>>", UtilDAOMapperContextSbr.toString());
        	
        	StringBuffer selectSequenceNumberContextSbr = new StringBuffer();
        	if(codeGenConfig.getDbConnection().getType().equals("MSSQL")){
        		selectSequenceNumberContextSbr.append("\n");
        		selectSequenceNumberContextSbr.append("      Declare @NewSeqVal int\n");
        		selectSequenceNumberContextSbr.append("      Exec @NewSeqVal =  " + strModuleName + "Module_SEQ\n");
        		selectSequenceNumberContextSbr.append("      select @NewSeqVal as NEXTVAL\n");
        	}else{
        		selectSequenceNumberContextSbr.append("      select " + strModuleName + "Module_SEQ.nextval from dual");
			}
        	context = context.replaceAll("<<selectSequenceNumber>>", selectSequenceNumberContextSbr.toString());
        }
            
        FileWriter fileOut = new FileWriter(ObjectFileName, false);
        fileOut.write(context);
        fileOut.flush();
        fileOut.close();
	    
	}
	
	/**
	* 產生Table Property
	*/
	public static void generateTableProperty(String strModuleName, Hashtable<String, String> hstTableByObjName) throws Exception {
		//建立Package路徑
	    File filePath = new File("./src/" + codeGenConfig.getObject().getPackage().replaceAll("\\.", "\\/"));
		if(!filePath.exists()){
			filePath.mkdirs();
		}

		Set<String> set = hstTableByObjName.keySet();
		Iterator<String> itr = set.iterator();
        String TablePropertyContext = "";
        StringBuffer TablePropertyContextSbr = new StringBuffer();
        TablePropertyContextSbr.append("\n");
        
		while(itr.hasNext()){
			String key = itr.next();
			String theTableName = key;
			String theObjectName = (String)hstTableByObjName.get(key);

			TablePropertyContextSbr.append(theObjectName + " = " + theTableName + "\n");
		}

		TablePropertyContext = "";
		TablePropertyContext = readTemplateFile("TablePropertyTemplate.txt");
		
        if(!"".equals(TablePropertyContext)){
        	TablePropertyContext = TablePropertyContext.replaceAll("<<ModuleTableProperty>>", strModuleName + "ModuleSeq = " + strModuleName + "Module_SEQ");
        	TablePropertyContext = TablePropertyContext.replaceAll("<<ObjTableProperty>>", TablePropertyContextSbr.toString());
        }
        
        String ObjectFileName = "./src/" + codeGenConfig.getObject().getPackage().replaceAll("\\.", "\\/").toLowerCase() + "/table.properties";
            
        FileWriter fileOut = new FileWriter(ObjectFileName, false);
        fileOut.write(TablePropertyContext);
        fileOut.flush();
        fileOut.close();
	    
	}
	
	/**
	* 產生UtilDAO Mapper Condition
	*/
	public static String generateUtilDAOMapperCondition(String theTableName, String theObjectName, ArrayList<String> alTableColumnName, ArrayList<String> alTableColumnType) throws Exception {
		//建立DAO Mapper Condition
        StringBuffer strbReturn = new StringBuffer();
        
	    if(alTableColumnName != null && alTableColumnName.size() > 0){
	    	strbReturn.append(genUtilDAOMapperModelBaseField(alTableColumnName, alTableColumnType, theTableName));
           	strbReturn.append(genUtilDAOMapperSearhDateField(alTableColumnName, alTableColumnType, theTableName));
           	strbReturn.append(genUtilDAOMapperSearhArrayField(alTableColumnName, alTableColumnType, theTableName));
            strbReturn.append(genUtilDAOMapperCusSearhField(alTableColumnName, alTableColumnType, theTableName));

	    }
	    
	    return strbReturn.toString();
	}
	
	/**
     * 產生UtilDAOMapper ModelBase欄位程式碼
     * @return
     */
    public static String genUtilDAOMapperModelBaseField(ArrayList<String> alTableColumnName, ArrayList<String> alTableColumnType, String tableName) {
        StringBuffer strbReturn = new StringBuffer();
        strbReturn.append("\n");
        
        for(int i=0; i<alTableColumnName.size(); i++){
        	String strColName = (String)alTableColumnName.get(i).toString();
        	String strColNameLowCase = strColName.toLowerCase();
        	String strColType = (String)alTableColumnType.get(i).toString();
        	
         	//Blob欄位不產生為條件
        	if(!strColName.toLowerCase().equals("fl")){
        	  	strbReturn.append("           <if test=\"" + strColNameLowCase + " != '' \">\n");
            	if(i == 0){
            		strbReturn.append("              " + strColName + " = #{" + strColNameLowCase + ",jdbcType=" + strColType + "}\n");
            	}else{
            		strbReturn.append("              AND " + strColName + " = #{" + strColNameLowCase + ",jdbcType=" + strColType + "}\n");
            	}
            	strbReturn.append("           </if>\n");
        	}
        }
        
        return strbReturn.toString();
    }
	
	 /**
     * 產生UtilDAOMapper日期區間查詢資料
     * @return
     */
    public static String genUtilDAOMapperSearhDateField(ArrayList<String> alTableColumnName, ArrayList<String> alTableColumnType, String tableName) {
        StringBuffer strbReturn = new StringBuffer();
    	for(int i=0; i<alTableColumnName.size(); i++){
    		 String strColName = alTableColumnName.get(i).toString();
    		 String strColNameLowCase = strColName.toLowerCase();
             String strColType = (String)alTableColumnType.get(i).toString();
            	
    		 //日期欄位才產生
    		 if(strColName.toLowerCase().indexOf("dat") >= 0 || strColName.toLowerCase().indexOf("date") >= 0 || strColName.toLowerCase().indexOf("tm") >= 0){
    			 String startDatColName = strColNameLowCase + "StartDate";
    			 String endDatColName = strColNameLowCase + "EndDate";
    		
	        	 strbReturn.append("           <if test=\"" + startDatColName + " != '' \">\n");
        		 strbReturn.append("              AND " + strColName + " <![CDATA[ >= ]]> #{" + startDatColName + ",jdbcType=" + strColType + "}\n");
	        	 strbReturn.append("           </if>\n");
	        	 strbReturn.append("           <if test=\"" + endDatColName + " != '' \">\n");
        		 strbReturn.append("              AND " + strColName + " <![CDATA[ <= ]]> #{" + endDatColName + ",jdbcType=" + strColType + "}\n");
	        	 strbReturn.append("           </if>\n");
       		 }
    	}
       
        return strbReturn.toString();
    }
    
    /**
     * 產生UtilDAOMapper查詢陣列條件查詢資料
     * @return
     */
    public static String genUtilDAOMapperSearhArrayField(ArrayList<String> alTableColumnName, ArrayList<String> alTableColumnType, String tableName) {
        StringBuffer strbReturn = new StringBuffer();
    	for(int i=0; i<alTableColumnName.size(); i++){
    		 String strColName = alTableColumnName.get(i).toString();
    		 String strColNameLowCase = strColName.toLowerCase();
    		 //狀態欄位產生
    		 if((strColName.toLowerCase().indexOf("tm") < 0 && strColName.toLowerCase().indexOf("xuid") < 0) && (strColName.toLowerCase().indexOf("sts") >= 0 || strColName.toLowerCase().indexOf("kd") >= 0 || strColName.toLowerCase().indexOf("type") >= 0)){
    			 String searhArrayColName = "srh" + strColNameLowCase + "Array";
        		
	        	 strbReturn.append("           <if test=\"" + searhArrayColName + " != null and " + searhArrayColName + ".size > 0 \">\n");
	        	 strbReturn.append("              AND\n");
	        	 strbReturn.append("              <foreach collection=\"" + searhArrayColName + "\" item=\"searhArrayField\" open=\"(\" close=\")\" separator=\"or\">\n");
        		 strbReturn.append("                 " + strColName + " = #{searhArrayField}\n");
	        	 strbReturn.append("              </foreach>\n");
	        	 strbReturn.append("           </if>\n");
    		 }
    	}
       
        return strbReturn.toString();
    }

 /**
     * 產生UtilDAOMapper自訂欄位資料
     * @return
     */
    public static String genUtilDAOMapperCusSearhField(ArrayList<String> alTableColumnName, ArrayList<String> alTableColumnType, String tableName) {
        StringBuffer strbReturn = new StringBuffer();
    	for(int i=0; i<alTableColumnName.size(); i++){
    		 String strColName = alTableColumnName.get(i).toString();
    		 String strColNameLowCase = strColName.toLowerCase();
             
    		 //自訂欄位資料產生(僅產生最多前十個)
    		 if(i < 10){
    			 String srhLikeColName = strColNameLowCase + "LikeSrh";
    			 String srhLikeIgnCaseColName = strColNameLowCase + "LikeIgnCaseSrh";
    			 String srhIgnCaseColName = strColNameLowCase + "IgnCaseSrh";
    			 String srhIsNotEmptyColName = strColNameLowCase + "IsNotEmptySrh";
    			 String srhIsEmptyColName = strColNameLowCase + "IsEmptySrh";
    			 
        		 if(strColName.toLowerCase().indexOf("fxuid") < 0 && strColName.toLowerCase().indexOf("fl") < 0 && strColName.toLowerCase().indexOf("kd") < 0 && strColName.toLowerCase().indexOf("type") < 0 && strColName.toLowerCase().indexOf("sts") < 0 && strColName.toLowerCase().indexOf("dat") < 0 && strColName.toLowerCase().indexOf("date") < 0 && strColName.toLowerCase().indexOf("tm") < 0 && strColName.toLowerCase().indexOf("xuid") < 0){
        			
        	         //Like Condition
    	        	 strbReturn.append("           <if test=\"" + srhLikeColName + " != '' \">\n");
    	        	 if(codeGenConfig.getDbConnection().getType().equals("MSSQL")){
    	        		 strbReturn.append("              AND " + strColName + " like '%'+#{" + srhLikeColName + "}+'%'\n");
    	        	 }else{
    	        		 strbReturn.append("              AND " + strColName + " like '%'||#{" + srhLikeColName + "}||'%'\n"); 
    	        	 }
    	        	 strbReturn.append("           </if>\n");
        	             
        	         //LikeIgnCase Condition
    	        	 strbReturn.append("           <if test=\"" + srhLikeIgnCaseColName + " != '' \">\n");
    	        	 if(codeGenConfig.getDbConnection().getType().equals("MSSQL")){
    	        		 strbReturn.append("              AND UPPER(" + strColName + ") like '%'+UPPER(#{" + srhLikeIgnCaseColName + "})+'%'\n");
    	        	 }else{
    	        		 strbReturn.append("              AND UPPER(" + strColName + ") like '%'||UPPER(#{" + srhLikeIgnCaseColName + "})||'%'\n"); 
    	        	 }
    	        	 strbReturn.append("           </if>\n");
    	      
        	         //IgnCase Condition
    	        	 strbReturn.append("           <if test=\"" + srhIgnCaseColName + " != '' \">\n");
            		 strbReturn.append("              AND UPPER(" + strColName + ") = UPPER(#{" + srhIgnCaseColName + "})\n");
    	        	 strbReturn.append("           </if>\n");
    	   
    	        	 //IsNotEmpty Condition
    	        	 strbReturn.append("           <if test=\"" + srhIsNotEmptyColName + " != '' \">\n");
    	           	 if(codeGenConfig.getDbConnection().getType().equals("MSSQL")){
                		 strbReturn.append("              AND (" + strColName + " is not null and " + strColName + " <![CDATA[ <> ]]> '')\n");
    	           	 }else{
                		 strbReturn.append("              AND (" + strColName + " is not null)\n");
    	           	 }
    	        	 strbReturn.append("           </if>\n");

    	        	 //IsEmpty Condition
    	        	 strbReturn.append("           <if test=\"" + srhIsEmptyColName + " != '' \">\n");
            		 strbReturn.append("              AND (" + strColName + " is null or " + strColName + " = '')\n");
    	        	 strbReturn.append("           </if>\n");
        		 }	 
    		 }else{
    			 break;
    		 }
    	}
       
        return strbReturn.toString();
    }
}
