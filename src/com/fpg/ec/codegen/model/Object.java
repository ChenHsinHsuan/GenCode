//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.2-147 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.05.20 at 03:27:09 下午 TST 
//


package com.fpg.ec.codegen.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{}Author"/>
 *         &lt;element ref="{}CreateBo"/>
 *         &lt;element ref="{}CreateDao"/>
 *         &lt;element ref="{}CreateMapper"/>
 *         &lt;element ref="{}CreateModel"/>
 *         &lt;element ref="{}CreateTableProperty"/>
 *         &lt;element ref="{}Module"/>
 *         &lt;element ref="{}Package"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "author",
    "createBo",
    "createDao",
    "createMapper",
    "createModel",
    "createTableProperty",
    "module",
    "_package"
})
@XmlRootElement(name = "Object")
public class Object {

    @XmlElement(name = "Author", required = true)
    protected String author;
    @XmlElement(name = "CreateBo", required = true)
    protected String createBo;
    @XmlElement(name = "CreateDao", required = true)
    protected String createDao;
    @XmlElement(name = "CreateMapper", required = true)
    protected String createMapper;
    @XmlElement(name = "CreateModel", required = true)
    protected String createModel;
    @XmlElement(name = "CreateTableProperty", required = true)
    protected String createTableProperty;
    @XmlElement(name = "Module", required = true)
    protected String module;
    @XmlElement(name = "Package", required = true)
    protected String _package;

    /**
     * Gets the value of the author property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Sets the value of the author property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAuthor(String value) {
        this.author = value;
    }

    /**
     * Gets the value of the createBo property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCreateBo() {
        return createBo;
    }

    /**
     * Sets the value of the createBo property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCreateBo(String value) {
        this.createBo = value;
    }

    /**
     * Gets the value of the createDao property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCreateDao() {
        return createDao;
    }

    /**
     * Sets the value of the createDao property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCreateDao(String value) {
        this.createDao = value;
    }

    /**
     * Gets the value of the createMapper property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCreateMapper() {
        return createMapper;
    }

    /**
     * Sets the value of the createMapper property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCreateMapper(String value) {
        this.createMapper = value;
    }

    /**
     * Gets the value of the createModel property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCreateModel() {
        return createModel;
    }

    /**
     * Sets the value of the createModel property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCreateModel(String value) {
        this.createModel = value;
    }

    /**
     * Gets the value of the createTableProperty property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCreateTableProperty() {
        return createTableProperty;
    }

    /**
     * Sets the value of the createTableProperty property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCreateTableProperty(String value) {
        this.createTableProperty = value;
    }

    /**
     * Gets the value of the module property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getModule() {
        return module;
    }

    /**
     * Sets the value of the module property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setModule(String value) {
        this.module = value;
    }

    /**
     * Gets the value of the package property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPackage() {
        return _package;
    }

    /**
     * Sets the value of the package property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPackage(String value) {
        this._package = value;
    }

}