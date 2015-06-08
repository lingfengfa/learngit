package com.xiaomi.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

public class CreatePolicyHql {
	
	public static String policyInputPath = "d:/policy.txt";     
	public static String policyHqlOutputPath = "d:/policy.hql";  //���ɵ�hql��Ӧ�����·����
	public static String endingFlagStr = ";";
	public static String lineSeparator = "\r\n";  //���з���
	
	public static void main(String []args){
		
		File file = new File(policyInputPath);
		BufferedReader bufReader = null;
		String line = null;
		PrintStream ps_out = null;
		try {
			ps_out = new PrintStream(new FileOutputStream(policyHqlOutputPath));
			System.setOut(ps_out); //��׼������ض���
			bufReader = new BufferedReader(new FileReader(file));
			
			while( (line=bufReader.readLine()) !=null){
				line = line.trim();
				if("[groups]".equals(line)){
					System.out.println("--[group]");
					dealGroup(bufReader); //���Ĵ���
				}else if("[roles]".equals(line)){
					System.out.println("--[roles]");
					dealRole(bufReader);  //���Ĵ���
				}else{
					continue;
				}
			}	
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			ps_out.close();
			try {
				bufReader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void  dealGroup(BufferedReader bufReader) throws Exception{
		
		String line = null;
		while( (line=bufReader.readLine()) !=null){
		//	System.out.println(line);
			line = line.trim();
			if("".equals(line))  //����Ҫ�� policy.txt�е�[roles]����һ��Ҫ����һ�����й���
				break;
			String arr[] = line.split("=");
			String group = arr[0].trim();
			String roles = arr[1].trim();
			String hqlStr = "";
			hqlStr = "grant role " + roles +" to group "+group+endingFlagStr;
			System.out.println(hqlStr);
		}
	}
	
	public static void dealRole(BufferedReader bufReader) throws Exception{
		
		String line = null; 
		String hql = "";
		String role = null;
		String subLine = null; 
		int num = 1;  //���ڱ�־�Ƿ�Ϊһ��role�Ŀ�ʼ��
		
		while( (line=bufReader.readLine()) !=null){
			line = line.trim(); 
			
			if("".equals(line) || line.startsWith("###")){  //�Ƿ�Ϊ���С� ����Щ######public managed automatically start######�����д������
				num = 1;
				continue;
			}else{
				if( line.endsWith(", \\")){
					line = line.substring(0, line.length()-3);
				}
				if( line.endsWith(",\\")){
					line = line.substring(0, line.length()-2);
				}
				if(num++==1){ //�����ǵ�һ�й�������Ҫ create role����
					int index = line.indexOf("=");
					role = line.substring(0, index).trim();
					subLine = line.substring(index+1, line.length()).trim();
	
					hql = "create role "+ role+endingFlagStr;
					hql += lineSeparator+createHql(subLine, role);  
					System.out.println(hql);
				}else{
					hql = createHql(line, role); 
					System.out.println(hql);
				}
			}
		}
	}
	
	public static String createHql(String subLine, String role){  //�����������͵ģ� server=server1->db=_dummy_database
		String hql = "";
		String arr[] = subLine.split("->");
		
		String key[] = new String[arr.length];
		String value[] = new String[arr.length];
		for(int i = 0;i < arr.length;i ++){
			String arr2[] = arr[i].split("=");
			key[i] = arr2[0].trim();
			value[i] = arr2[1].trim();
		}
		
		if(arr.length==1){  //server=server1
			hql = "grant all on server "+value[0]+" to role " + role;
		}else if(arr.length==2){ //server=server1->db=_dummy_database ����  server=server1->uri=file:///home/work/hiveserver/hive/auxlib 
			if("db".equals(key[1])){
				if("*".equals(value[1])) {
					hql = "--"+subLine+"  ���Ǻ���*�ŵģ���ʱ����������";
				}else  //��һ��server�е����ݿ������Ȩ�޸���ǰrole.
					hql = "grant all on database " + value[1]+" to role "+role; 
			}else if("uri".equals(key[1])){
				hql = "grant ALL on URI '"+value[1]+"' to "+role;  //�������role��?????
			}else{
				System.err.println("22����ô���ܱ�����أ�����������������Exception"); //��������ʱ����ʾ��Щ���û�п��ǵ���
			}
		}else if(arr.length==3){
			if("server".equals(key[0]) && "db".equals(key[1]) && "table".equals(key[2])){
				if("*".equals(value[1]) || "*".equals(value[2])){
					hql =  "--"+subLine+"  ���Ǻ���*�ŵģ���ʱ����������";
				}else{
					hql = "grant all on table "+value[2]+" on database " +value[1] +" to role "+role;
				}
			}else if("server".equals(key[0]) && "db".equals(key[1]) && "action".equals(key[2])){
				hql = "grant "+ value[2]+" on database "+ value[1] + " to role " + role;
			}else{
				System.err.println("33����ô���ܱ�����أ�����������������Exception"); //��������ʱ����ʾ��Щ���û�п��ǵ���
			}
		}else if(arr.length==4){
			if("server".equals(key[0]) && "db".equals(key[1]) && "table".equals(key[2]) && "action".equals(key[3])){
				if("*".equals(value[1]) || "*".equals(value[2]) || "*".equals("value[3]")){
					hql =  "--"+subLine+"  ���Ǻ���*�ŵģ���ʱ����������";
				}else{
					hql = "use database "+value[1]+";";
					hql += lineSeparator+"grant "+ value[3] +" on table "+ value[2] + " to role "+role;
				}
			}else{
				System.err.println("44����ô���ܱ�����أ�����������������Exception");//��������ʱ����ʾ��Щ���û�п��ǵ���
			}
		}
		return hql+endingFlagStr;
	}
}
