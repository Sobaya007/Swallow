package com.trap.swallow.server;

import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.Writer;

public class RequestJsonWriter extends JsonWriter {
	public RequestJsonWriter(Writer out) {
		super(out);
	}
	
	@Override
	public RequestJsonWriter beginArray() throws IOException {
		return (RequestJsonWriter) super.beginArray();
	}
	@Override
	public RequestJsonWriter endArray() throws IOException {
		return (RequestJsonWriter) super.endArray();
	}
	@Override
	public RequestJsonWriter beginObject() throws IOException {
		return (RequestJsonWriter) super.beginObject();
	}
	@Override
	public RequestJsonWriter endObject() throws IOException {
		return (RequestJsonWriter) super.endObject();
	}
	@Override
	public RequestJsonWriter name(String name) throws IOException {
		return (RequestJsonWriter) super.name(name);
	}
	@Override
	public RequestJsonWriter nullValue() throws IOException {
		return (RequestJsonWriter) super.nullValue();
	}
	@Override
	public RequestJsonWriter value(String value) throws IOException {
		if(value == null){
			return nullValue();
		}
		return (RequestJsonWriter) super.value(value);
	}
	@Override
	public RequestJsonWriter value(Number value) throws IOException {
		if(value == null){
			return nullValue();
		}
		return (RequestJsonWriter) super.value(value);
	}
	public RequestJsonWriter value(Boolean value) throws IOException {
		if(value == null){
			return nullValue();
		}
		return (RequestJsonWriter) super.value(value);
	}
	public RequestJsonWriter value(String[] value) throws IOException {
		if(value == null || value.length == 0){
			this.nullValue();
		}else{
			this.beginArray();
			for(String entry : value){
				this.value(entry);
			}
			this.endArray();
		}
		return (RequestJsonWriter) this;
	}
	public RequestJsonWriter value(Number[] value) throws IOException {
		if(value == null || value.length == 0){
			this.nullValue();
		}else{
			this.beginArray();
			for(Number entry : value){
				this.value(entry);
			}
			this.endArray();
		}
		return (RequestJsonWriter) this;
	}
}
