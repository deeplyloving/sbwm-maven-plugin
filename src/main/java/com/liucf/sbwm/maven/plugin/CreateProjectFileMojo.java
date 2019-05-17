package com.liucf.sbwm.maven.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.maven.model.Profile;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Goal which touches a timestamp file.
 *
 */
@Mojo(name = "crtpf", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class CreateProjectFileMojo extends AbstractMojo {
	/**
	 * Location of the file.
	 */
	@Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = true)
	private File outputDirectory;
	
	@Parameter
	private List<String> modulePaths;
	
	@Parameter
	private String targetPropPath="src/main/resources/application.properties";
	
	@Parameter
    private Map<String,String> overriedProp;
	
	@Parameter
	private String profileActive;
	

	public void execute() throws MojoExecutionException {
		MavenProject project = (MavenProject) getPluginContext().get("project");
		if(project.getActiveProfiles()!=null && project.getActiveProfiles().size()>0) {
			profileActive = ((Profile)project.getActiveProfiles().get(0)).getId();
			getLog().info("当前profile:"+profileActive);
		}
		if(modulePaths!=null) {
			List<File> files = new ArrayList<File>();
			List<File> profileFiles = new ArrayList<File>();
			for (String modulePath : modulePaths) {
				File folder = new File(project.getBasedir()+"/"+modulePath,"src/main/resources");
				if(folder.exists()) {
					File file = new File(folder,"application.properties");
					if(file.exists()) {
						files.add(file);
					}else {
						getLog().info(String.format("文件不存在![%s]",file.getPath()));
					}
					if(profileActive!=null && profileActive.trim().length()!=0) {
						File afile = new File(folder,String.format("application-%s.properties",profileActive));
						if(afile.exists()) {
							profileFiles.add(afile);
						}else {
							getLog().info(String.format("文件不存在![%s]",afile.getPath()));
						}
					}
				}else {
					getLog().info(String.format("文件夹不存在![%s]",folder.getPath()));
				}
			}
			files.addAll(profileFiles);
			try {
				PropertiesConfiguration config = null;
				for (File file : files) {
					getLog().info("合并:"+file.getPath());
					if(config == null) {
						config = new PropertiesConfiguration(file);
					}else {
						PropertiesConfiguration tmp = new PropertiesConfiguration(file);
						Iterator keys = tmp.getKeys();
						while(keys.hasNext()) {
							String next = (String) keys.next();
							config.setProperty(next, tmp.getProperty(next));
						}
					}
				}
				if(config != null) {
					if(overriedProp!=null) {
						for(Map.Entry<String, String> entry : overriedProp.entrySet()) {
							config.setProperty(entry.getKey(), entry.getValue());
						}
					}
					config.save(new File(targetPropPath));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}else {
			getLog().info("未指定模块");
		}
	}
}
