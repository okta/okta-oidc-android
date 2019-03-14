<xsl:stylesheet	version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:java="http://example.com/namespace">

    <xsl:function name="java:get-package-name" as="xs:string">
        <xsl:param name="fileName"/>
        <xsl:variable name="relativeFileName" select="substring-after($fileName, '/src/main/java/')"/>
        <xsl:variable name="packagePath" select="
        substring($relativeFileName,
                  1,
                  index-of(string-to-codepoints($relativeFileName),
                           string-to-codepoints('/')
                  )[last()] -1)"/>
        <xsl:value-of select="translate($packagePath, '/', '.')"/>
    </xsl:function>

    <xsl:function name="java:get-class-name" as="xs:string">
        <xsl:param name="fileName"/>
        <xsl:variable name="relativeFileName" select="substring-after($fileName, '/src/main/java/')"/>
        <xsl:value-of select="translate($relativeFileName, '/', '.')"/>
    </xsl:function>

    <xsl:function name="java:get-short-class-name" as="xs:string">
        <xsl:param name="fileName"/>
        <xsl:value-of select="tokenize($fileName, '/')[last()]"/>
    </xsl:function>

    <xsl:function name="java:get-class-from-package" as="xs:string">
        <xsl:param name="fullyQualifiedName"/>
        <xsl:value-of select="tokenize($fullyQualifiedName, '\.')[last()]"/>
    </xsl:function>

    <xsl:output method="html" indent="yes"/>
    <xsl:decimal-format decimal-separator="." grouping-separator="," />

    <xsl:key name="files" match="file" use="@name" />
    <xsl:key name="packages" match="file" use="java:get-package-name(@name)"/>

    <xsl:template match="checkstyle">
        <xsl:variable name="errorCount" select="count(file/error)"/>
        <xsl:variable name="fileCount" select="count(file[@name and generate-id(.) = generate-id(key('files', @name))])"/>
        <html>
            <head>
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
                <title>Checkstyle Report</title>
                <link rel="stylesheet" href="https://fonts.googleapis.com/icon?family=Material+Icons"/>
                <link rel="stylesheet" href="https://code.getmdl.io/1.2.1/material.blue-indigo.min.css" />
                <link rel="stylesheet" href="http://fonts.googleapis.com/css?family=Roboto:300,400,500,700" type="text/css"/>
                <script defer="defer" src="https://code.getmdl.io/1.2.0/material.min.js"/>
                <style>
                    section.section--center {
                    max-width: 860px;
                    }
                    .mdl-card__supporting-text + .mdl-card__actions {
                    border-top: 1px solid rgba(0, 0, 0, 0.12);
                    }
                    main > .mdl-layout__tab-panel {
                    padding: 8px;
                    padding-top: 48px;
                    }

                    .mdl-card__actions {
                    margin: 0;
                    padding: 4px 40px;
                    color: inherit;
                    }
                    .mdl-card > * {
                    height: auto;
                    }
                    .mdl-card__actions a {
                    color: #00BCD4;
                    margin: 0;
                    }
                    .mdl-chip {
                    margin: 2px;
                    }
                    .error-icon {
                    color: #bb7777;
                    vertical-align: bottom;
                    }
                    .warning-icon {
                    vertical-align: bottom;
                    }
                    .mdl-layout__content section:not(:last-of-type) {
                    position: relative;
                    margin-bottom: 48px;
                    }

                    .mdl-card .mdl-card__supporting-text {
                    margin: 40px;
                    -webkit-flex-grow: 1;
                    -ms-flex-positive: 1;
                    flex-grow: 1;
                    padding: 0;
                    color: inherit;
                    width: calc(100% - 80px);
                    }
                    div.mdl_layout__drawer {
                    400px;
                    }
                    div.mdl-layout__drawer-button .material-icons {
                    line-height: 48px;
                    }
                    .mdl-card .mdl-card__supporting-text {
                    margin-top: 0px;
                    }
                    .chips {
                    float: right;
                    vertical-align: middle;
                    }
                    pre.errorlines {
                    background-color: white;
                    font-family: monospace;
                    border: 1px solid #e0e0e0;
                    line-height: 0.9rem;
                    font-size: 0.9rem;    padding: 1px 0px 1px; 1px;
                    overflow: scroll;
                    }
                    .prefix {
                    color: #660e7a;
                    font-weight: bold;
                    }
                    .attribute {
                    color: #0000ff;
                    font-weight: bold;
                    }
                    .value {
                    color: #008000;
                    font-weight: bold;
                    }
                    .tag {
                    color: #000080;
                    font-weight: bold;
                    }
                    .comment {
                    color: #808080;
                    font-style: italic;
                    }
                    .javadoc {
                    color: #808080;
                    font-style: italic;
                    }
                    .annotation {
                    color: #808000;
                    }
                    .string {
                    color: #008000;
                    font-weight: bold;
                    }
                    .number {
                    color: #0000ff;
                    }
                    .keyword {
                    color: #000080;
                    font-weight: bold;
                    }
                    .caretline {
                    background-color: #fffae3;
                    }
                    .lineno {
                    color: #999999;
                    background-color: #f0f0f0;
                    }
                    .warninglist-error {
                    margin-bottom: 1em;
                    }
                    .error {
                    display: inline-block;
                    position:relative;
                    background: url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAQAAAAECAYAAACp8Z5+AAAABmJLR0QA/wD/AP+gvaeTAAAACXBIWXMAAAsTAAALEwEAmpwYAAAAB3RJTUUH4AwCFR4T/3uLMgAAADxJREFUCNdNyLERQEAABMCjL4lQwIzcjErpguAL+C9AvgKJDbeD/PRpLdm35Hm+MU+cB+tCKaJW4L4YBy+CAiLJrFs9mgAAAABJRU5ErkJggg==) bottom repeat-x;
                    }
                    .warning {
                    text-decoration: none;
                    background-color: #f6ebbc;
                    }
                    .overview {
                    padding: 10pt;
                    width: 100%;
                    overflow: auto;
                    border-collapse:collapse;
                    }
                    .overview tr {
                    border-bottom: solid 1px #eeeeee;
                    }
                    .categoryColumn a {
                    text-decoration: none;
                    color: inherit;
                    }
                    .countColumn {
                    text-align: right;
                    padding-right: 20px;
                    width: 50px;
                    }
                    .issueColumn {
                    padding-left: 16px;
                    }
                    .issueColumn a {
                    padding-left: 4px;
                    }
                    .categoryColumn {
                    position: relative;
                    left: -50px;
                    padding-top: 20px;
                    padding-bottom: 5px;
                    }
                </style>
                <script language="javascript" type="text/javascript">
                    function reveal(id) {
                      if (document.getElementById) {
                        document.getElementById(id).style.display = 'block';
                        document.getElementById(id+'Link').style.display = 'none';
                      }
                    }
                    function hideid(id) {
                      if (document.getElementById) {
                        document.getElementById(id).style.display = 'none';
                      }
                    }
                </script>
            </head>
            <body class="mdl-color--grey-100 mdl-color-text--grey-700 mdl-base">
                <div class="mdl-layout mdl-js-layout mdl-layout--fixed-header">
                    <header class="mdl-layout__header">
                        <div class="mdl-layout__header-row">
                            <span class="mdl-layout-title">Checkstyle Report: <xsl:value-of select="$errorCount"/>
                                <xsl:choose>
                                    <xsl:when test="$errorCount = 1">
                                        error
                                    </xsl:when>
                                    <xsl:otherwise>
                                        errors
                                    </xsl:otherwise>
                                </xsl:choose>
                            </span>
                            <div class="mdl-layout-spacer" />
                            <nav class="mdl-navigation mdl-layout--large-screen-only">
                                Check performed on
                                <!--e.g. Wednesday August 09 at 22:30:05 PDT 2017-->
                                <xsl:value-of select="format-dateTime(current-dateTime(), '[F] [MNn] [D] at [H]:[m]:[s] [ZN] [Y]')"/>
                            </nav>
                        </div>
                    </header>
                    <div class="mdl-layout__drawer">
                        <span class="mdl-layout-title">Files</span>
                        <nav class="mdl-navigation">
                            <a class="mdl-navigation__link" href="#overview"><i class="material-icons">dashboard</i>Overview</a>

                            <xsl:if test="$errorCount &gt; 0">
                                <xsl:for-each-group select="file" group-by="java:get-package-name(@name)">
                                    <a class="mdl-navigation__link" href="#{current-grouping-key()}">
                                        <i class="material-icons error-icon">error</i>
                                        <xsl:value-of select="current-grouping-key()"/>
                                    </a>
                                </xsl:for-each-group>
                            </xsl:if>

                        </nav>
                    </div>
                    <main class="mdl-layout__content">
                        <div class="mdl-layout__tab-panel is-active">

                            <!-- OVERVIEW -->
                            <a name="overview"/>
                            <section class="section--center mdl-grid mdl-grid--no-spacing mdl-shadow--2dp" id="card0" style="display: block;">
                                <div class="mdl-card mdl-cell mdl-cell--12-col">
                                    <xsl:choose>
                                        <xsl:when test="$errorCount = 0">
                                            <div class="mdl-card__title">
                                                <h2 class="mdl-card__title-text">No Issues Found</h2>
                                            </div>
                                            <div class="mdl-card__supporting-text">Congratulations!</div>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <div class="mdl-card__title">
                                                <h2 class="mdl-card__title-text">Overview</h2>
                                            </div>
                                            <div class="mdl-card__supporting-text">
                                                <table class="overview">
                                                    <xsl:apply-templates select="." mode="overviewTable"/>
                                                </table>
                                                <br/>
                                            </div>
                                            <div class="mdl-card__actions mdl-card--border">
                                                <button class="mdl-button mdl-js-button mdl-js-ripple-effect" id="card0Link" onclick="hideid('card0');">
                                                    Dismiss</button>
                                            </div>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </div>
                            </section>

                            <!-- PACKAGES -->
                            <xsl:if test="$errorCount &gt; 0">
                                <xsl:for-each-group select="file" group-by="java:get-package-name(@name)">
                                    <a name="{current-grouping-key()}"/>
                                    <xsl:for-each select="current-group()">
                                        <xsl:apply-templates select="." mode="fileCard"/>
                                    </xsl:for-each>
                                </xsl:for-each-group>
                            </xsl:if>
                        </div>
                    </main>
                </div>
            </body>
        </html>
    </xsl:template>
    
    <xsl:template match="checkstyle" mode="overviewTable">
        <xsl:for-each-group select="file" group-by="java:get-package-name(@name)">
            <xsl:if test="count(key('packages', current-grouping-key())/error) &gt; 0">
                <tr>
                    <td class="countColumn"/>
                    <td class="categoryColumn">
                        <a href="#{current-grouping-key()}">
                            <xsl:value-of select="current-grouping-key()"/>
                        </a>
                    </td>
                </tr>
                <xsl:for-each select="current-group()">
                    <xsl:if test="count(./error) &gt; 0">
                        <tr>
                            <td class="countColumn"><xsl:value-of select="count(key('files', @name)/error)"/></td>
                            <td class="issueColumn">
                                <i class="material-icons error-icon">error</i>
                                <a href="#{java:get-class-name(@name)}"><xsl:value-of select="java:get-short-class-name(@name)"/></a>
                            </td>
                        </tr>
                    </xsl:if>
                </xsl:for-each>
            </xsl:if>
        </xsl:for-each-group>
    </xsl:template>

    <xsl:template match="file" mode="fileCard">
        <xsl:variable name="className" select="java:get-class-name(@name)"/>
        <xsl:if test="count(./error) &gt; 0">
            <a name="{$className}"/>
            <section class="section--center mdl-grid mdl-grid--no-spacing mdl-shadow--2dp" id="{$className}-card" style="display: block;">
                <div class="mdl-card mdl-cell mdl-cell--12-col">
                    <div class="mdl-card__title">
                        <h2 class="mdl-card__title-text"><xsl:value-of select="$className"/></h2>
                    </div>
                    <div class="mdl-card__supporting-text">
                        <div class="issue">
                            <div class="warningslist">
                                <xsl:for-each select="./error">
                                    <div class="warninglist-error">
                                        <span class="location">Line <xsl:value-of select="@line"/></span>:&#160;
                                        <span class="message"><xsl:value-of select="@message"/></span>
                                        <br/>
                                    </div>
                                </xsl:for-each>
                            </div>
                        </div>
                        <div class="chips">
                            <xsl:for-each-group select="./error" group-by="@source">
                                <span class="mdl-chip">
                                    <span class="mdl-chip__text"><xsl:value-of select="java:get-class-from-package(current-grouping-key())"/></span>
                                </span>
                            </xsl:for-each-group>
                        </div>
                    </div>
                    <div class="mdl-card__actions mdl-card--border">
                        <button class="mdl-button mdl-js-button mdl-js-ripple-effect" id="{$className}-cardLink" onclick="hideid('{$className}-card');">
                            Dismiss
                        </button>
                    </div>
                </div>
            </section>
        </xsl:if>
    </xsl:template>
</xsl:stylesheet>
