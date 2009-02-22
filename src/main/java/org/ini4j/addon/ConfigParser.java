/**
 * Copyright 2005,2009 Ivan SZKIBA
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
package org.ini4j.addon;

import org.ini4j.Config;
import org.ini4j.Ini;
import org.ini4j.IniHandler;
import org.ini4j.InvalidIniFormatException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import java.net.URL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigParser
{
    private PyIni _ini;

    @SuppressWarnings("unchecked")
    public ConfigParser()
    {
        this(Collections.EMPTY_MAP);
    }

    public ConfigParser(Map<String, String> defaults)
    {
        _ini = new PyIni(defaults);
    }

    public boolean getBoolean(String section, String option) throws NoSectionException, NoOptionException, InterpolationException
    {
        boolean ret;
        String value = get(section, option);

        if ("1".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value))
        {
            ret = true;
        }
        else if ("0".equalsIgnoreCase(value) || "no".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value) || "off".equalsIgnoreCase(value))
        {
            ret = false;
        }
        else
        {
            throw new IllegalArgumentException(value);
        }

        return ret;
    }

    public double getDouble(String section, String option) throws NoSectionException, NoOptionException, InterpolationException
    {
        return Double.parseDouble(get(section, option));
    }

    public float getFloat(String section, String option) throws NoSectionException, NoOptionException, InterpolationException
    {
        return Float.parseFloat(get(section, option));
    }

    public int getInt(String section, String option) throws NoSectionException, NoOptionException, InterpolationException
    {
        return Integer.parseInt(get(section, option));
    }

    public long getLong(String section, String option) throws NoSectionException, NoOptionException, InterpolationException
    {
        return Long.parseLong(get(section, option));
    }

    public void addSection(String section) throws DuplicateSectionException
    {
        if (_ini.containsKey(section))
        {
            throw new DuplicateSectionException(section);
        }
        else if (PyIni.DEFAULT_SECTION_NAME.equalsIgnoreCase(section))
        {
            throw new IllegalArgumentException(section);
        }

        _ini.add(section);
    }

    public Map<String, String> defaults()
    {
        return _ini.getDefaults();
    }

    @SuppressWarnings("unchecked")
    public String get(String section, String option) throws NoSectionException, NoOptionException, InterpolationException
    {
        return get(section, option, false, Collections.EMPTY_MAP);
    }

    @SuppressWarnings("unchecked")
    public String get(String section, String option, boolean raw) throws NoSectionException, NoOptionException, InterpolationException
    {
        return get(section, option, raw, Collections.EMPTY_MAP);
    }

    public String get(String sectionName, String optionName, boolean raw, Map<String, String> variables) throws NoSectionException, NoOptionException, InterpolationException
    {
        String value = requireOption(sectionName, optionName);

        if (!raw && (value != null) && (value.indexOf(PyIni.SUBST_CHAR) >= 0))
        {
            value = _ini.fetch(sectionName, optionName, variables);
        }

        return value;
    }

    public boolean hasOption(String sectionName, String optionName)
    {
        Ini.Section section = _ini.get(sectionName);

        return (section != null) && section.containsKey(optionName);
    }

    public boolean hasSection(String sectionName)
    {
        return _ini.containsKey(sectionName);
    }

    @SuppressWarnings("unschecked")
    public List<Map.Entry<String, String>> items(String sectionName) throws NoSectionException, InterpolationMissingOptionException
    {
        return items(sectionName, false, Collections.EMPTY_MAP);
    }

    @SuppressWarnings("unschecked")
    public List<Map.Entry<String, String>> items(String sectionName, boolean raw) throws NoSectionException, InterpolationMissingOptionException
    {
        return items(sectionName, raw, Collections.EMPTY_MAP);
    }

    public List<Map.Entry<String, String>> items(String sectionName, boolean raw, Map<String, String> variables) throws NoSectionException, InterpolationMissingOptionException
    {
        Ini.Section section = requireSection(sectionName);
        Map<String, String> ret;

        if (raw)
        {
            ret = new HashMap<String, String>(section);
        }
        else
        {
            ret = new HashMap<String, String>();
            for (String key : section.keySet())
            {
                ret.put(key, _ini.fetch(section, key, variables));
            }
        }

        return new ArrayList<Map.Entry<String, String>>(ret.entrySet());
    }

    public void read(String... filenames) throws IOException, ParsingException
    {
        for (String filename : filenames)
        {
            read(new File(filename));
        }
    }

    public void read(Reader reader) throws IOException, ParsingException
    {
        try
        {
            _ini.load(reader);
        }
        catch (InvalidIniFormatException x)
        {
            throw new ParsingException(x.getMessage());
        }
    }

    public void read(URL url) throws IOException, ParsingException
    {
        try
        {
            _ini.load(url);
        }
        catch (InvalidIniFormatException x)
        {
            throw new ParsingException(x.getMessage());
        }
    }

    public void read(File file) throws IOException, ParsingException
    {
        try
        {
            _ini.load(new FileReader(file));
        }
        catch (InvalidIniFormatException x)
        {
            throw new ParsingException(x.getMessage());
        }
    }

    public void read(InputStream stream) throws IOException, ParsingException
    {
        try
        {
            _ini.load(stream);
        }
        catch (InvalidIniFormatException x)
        {
            throw new ParsingException(x.getMessage());
        }
    }

    public boolean removeOption(String sectionName, String optionName) throws NoSectionException
    {
        Ini.Section section = requireSection(sectionName);
        boolean ret = section.containsKey(optionName);

        section.remove(optionName);

        return ret;
    }

    public boolean removeSection(String sectionName)
    {
        boolean ret = _ini.containsKey(sectionName);

        _ini.remove(sectionName);

        return ret;
    }

    public List<String> sections()
    {
        return new ArrayList<String>(_ini.keySet());
    }

    public void set(String sectionName, String optionName, Object value) throws NoSectionException
    {
        Ini.Section section = requireSection(sectionName);

        if (value == null)
        {
            section.remove(optionName);
        }
        else
        {
            section.put(optionName, value.toString());
        }
    }

    public void write(Writer writer) throws IOException
    {
        _ini.store(writer);
    }

    public void write(OutputStream stream) throws IOException
    {
        _ini.store(stream);
    }

    public void write(File file) throws IOException
    {
        _ini.store(new FileWriter(file));
    }

    protected PyIni getIni()
    {
        return _ini;
    }

    private String requireOption(String sectionName, String optionName) throws NoSectionException, NoOptionException
    {
        Ini.Section section = requireSection(sectionName);
        String option = section.get(optionName);

        if (option == null)
        {
            throw new NoOptionException(optionName);
        }

        return option;
    }

    private Ini.Section requireSection(String sectionName) throws NoSectionException
    {
        Ini.Section section = _ini.get(sectionName);

        if (section == null)
        {
            throw new NoSectionException(sectionName);
        }

        return section;
    }

    public static class ConfigParserException extends Exception
    {
        public ConfigParserException(String message)
        {
            super(message);
        }
    }

    public static class DuplicateSectionException extends ConfigParserException
    {
        private DuplicateSectionException(String message)
        {
            super(message);
        }
    }

    public static class InterpolationException extends ConfigParserException
    {
        private InterpolationException(String message)
        {
            super(message);
        }
    }

    public static class InterpolationMissingOptionException extends InterpolationException
    {
        private InterpolationMissingOptionException(String message)
        {
            super(message);
        }
    }

    public static class NoOptionException extends ConfigParserException
    {
        private NoOptionException(String message)
        {
            super(message);
        }
    }

    public static class NoSectionException extends ConfigParserException
    {
        private NoSectionException(String message)
        {
            super(message);
        }
    }

    public static class ParsingException extends IOException
    {
        private ParsingException(String message)
        {
            super(message);
        }
    }

    protected static class PyIni extends Ini
    {
        private static final char SUBST_CHAR = '%';
        private static final Pattern expr = Pattern.compile("(?<!\\\\)\\%\\(([^\\)]+)\\)");
        private static final int G_OPTION = 1;
        protected static final String DEFAULT_SECTION_NAME = "DEFAULT";
        private Map<String, String> _defaults;
        private Ini.Section _defaultSection;

        public PyIni(Map<String, String> defaults)
        {
            _defaults = defaults;
            Config cfg = getConfig().clone();

            cfg.setEscape(false);
            cfg.setMultiOption(false);
            cfg.setMultiSection(false);
            cfg.setLowerCaseOption(true);
            cfg.setLowerCaseSection(true);
            super.setConfig(cfg);
        }

        @Deprecated @Override
        @SuppressWarnings("empty-statement")
        public void setConfig(Config value)
        {
            ;
        }

        public Map<String, String> getDefaults()
        {
            return _defaults;
        }

        @Override public Section add(String name)
        {
            Section section;

            if (DEFAULT_SECTION_NAME.equalsIgnoreCase(name))
            {
                if (_defaultSection == null)
                {
                    _defaultSection = new Ini.Section(name);
                }

                section = _defaultSection;
            }
            else
            {
                section = super.add(name);
            }

            return section;
        }

        public String fetch(String sectionName, String optionName, Map<String, String> variables) throws InterpolationMissingOptionException
        {
            return fetch(get(sectionName), optionName, variables);
        }

        protected Ini.Section getDefaultSection()
        {
            return _defaultSection;
        }

        protected String fetch(Ini.Section section, String optionName, Map<String, String> variables) throws InterpolationMissingOptionException
        {
            String value = section.get(optionName);

            if ((value != null) && (value.indexOf(SUBST_CHAR) >= 0))
            {
                StringBuilder buffer = new StringBuilder(value);

                resolve(buffer, section, variables);
                value = buffer.toString();
            }

            return value;
        }

        protected void resolve(StringBuilder buffer, Ini.Section owner, Map<String, String> vars) throws InterpolationMissingOptionException
        {
            Matcher m = expr.matcher(buffer);

            while (m.find())
            {
                String optionName = m.group(G_OPTION);
                String value = owner.get(optionName);

                if (value == null)
                {
                    value = vars.get(optionName);
                }

                if (value == null)
                {
                    value = _defaults.get(optionName);
                }

                if ((value == null) && (_defaultSection != null))
                {
                    value = _defaultSection.get(optionName);
                }

                if (value == null)
                {
                    throw new InterpolationMissingOptionException(optionName);
                }

                buffer.replace(m.start(), m.end(), value);
                m.reset(buffer);
            }
        }

        @Override protected void store(IniHandler formatter) throws IOException
        {
            formatter.startIni();
            if (_defaultSection != null)
            {
                store(formatter, _defaultSection);
            }

            for (Ini.Section s : values())
            {
                store(formatter, s);
            }

            formatter.endIni();
        }

        protected void store(IniHandler formatter, Section section) throws IOException
        {
            formatter.startSection(section.getName());
            for (String name : section.keySet())
            {
                formatter.handleOption(name, section.get(name));
            }

            formatter.endSection();
        }
    }
}