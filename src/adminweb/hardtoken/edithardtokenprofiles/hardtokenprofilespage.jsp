<%
  TreeMap hardtokenprofiles = ejbcawebbean.getInformationMemory().getHardTokenProfiles(); 
  
  
%>


<div align="center">
  <p><H1><%= ejbcawebbean.getText("EDITHARDTOKENPROFILES") %></H1></p>
 <!-- <div align="right"><A  onclick='displayHelpWindow("<%= ejbcawebbean.getHelpfileInfix("ca_help.html") + "#certificateprofiles"%>")'>
    <u><%= ejbcawebbean.getText("HELP") %></u> </A> -->
  </div>
  <form name="editprofiles" method="post"  action="<%= THIS_FILENAME%>">
    <input type="hidden" name='<%= helper.ACTION %>' value='<%=helper.ACTION_EDIT_HARDTOKENPROFILES %>'>
    <table width="100%" border="0" cellspacing="0" cellpadding="0">
    <% if(helper.hardtokenprofileexists){ 
          helper.hardtokenprofileexists= false;%> 
      <tr> 
        <td width="5%"></td>
        <td width="60%"><H4 id="alert"><%= ejbcawebbean.getText("HARDTOKENPROFILEALREADY") %></H4></td>
        <td width="35%"></td>
      </tr>
    <% } %>
    <% if(helper.hardtokenprofiledeletefailed){
          helper.hardtokenprofiledeletefailed = false; 
          %> 
      <tr> 
        <td width="5%"></td>
        <td width="60%"><H4 id="alert"><%= ejbcawebbean.getText("COULDNTDELETEHARDTOKENPROF") %></H4></td>
        <td width="35%"></td>
      </tr>
    <% } %>
      <tr> 
        <td width="5%"></td>
        <td width="60%"><H3><%= ejbcawebbean.getText("CURRENTHARDTOKENPROFILES") %></H3></td>
        <td width="35%"></td>
      </tr>
      <tr> 
        <td width="5%"></td>
        <td width="60%">
          <select name="<%=EditHardTokenProfileJSPHelper.SELECT_HARDTOKENPROFILES%>" size="15"  >
            <% Iterator iter = hardtokenprofiles.keySet().iterator();
               while(iter.hasNext()){
                 String profilename = (String) iter.next(); %>
                 
              <option value="<%=profilename%>"> 
                  <%= profilename %>
               </option>
            <%}%>
              <option value="">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</option>
          </select>
          </td>
      </tr>
      <tr> 
        <td width="5%"></td>
        <td width="60%"> 
          <table width="100%" border="0" cellspacing="0" cellpadding="0">
            <tr>
              <td>
                <input type="submit" name="<%= helper.BUTTON_EDIT_HARDTOKENPROFILES %>" value="<%= ejbcawebbean.getText("EDITHARDTOKENPROF") %>">
              </td>
              <td>
             &nbsp; 
              </td>
              <td>
                <input class=buttonstyle type="submit" onClick="return confirm('<%= ejbcawebbean.getText("AREYOUSURE") %>');" name="<%= helper.BUTTON_DELETE_HARDTOKENPROFILES %>" value="<%= ejbcawebbean.getText("DELETEHARDTOKENPROF") %>">
              </td>
            </tr>
          </table> 
        </td>
        <td width="35%"> </td>
      </tr>
    </table>
   
  <p align="left"> </p>
    <table width="100%" border="0" cellspacing="0" cellpadding="0">
      <tr> 
        <td width="5%"></td>
        <td width="95%"><H3><%= ejbcawebbean.getText("ADD") %></H3></td>
      </tr>
      <tr> 
        <td width="5%"></td>
        <td width="95%"> 
          <input type="text" name="<%= helper.TEXTFIELD_HARDTOKENPROFILESNAME%>" size="40" maxlength="255">   
          <input type="submit" name="<%= helper.BUTTON_ADD_HARDTOKENPROFILES%>" onClick='return checkfieldforlegalchars("document.editprofiles.<%=helper.TEXTFIELD_HARDTOKENPROFILESNAME%>","<%= ejbcawebbean.getText("ONLYCHARACTERS") %>")' value="<%= ejbcawebbean.getText("ADD") %>">&nbsp;&nbsp;&nbsp;
          <input type="submit" name="<%= helper.BUTTON_RENAME_HARDTOKENPROFILES%>" onClick='return checkfieldforlegalchars("document.editprofiles.<%=helper.TEXTFIELD_HARDTOKENPROFILESNAME%>","<%= ejbcawebbean.getText("ONLYCHARACTERS") %>")' value="<%= ejbcawebbean.getText("RENAMESELECTED") %>">&nbsp;&nbsp;&nbsp;
          <input type="submit" name="<%= helper.BUTTON_CLONE_HARDTOKENPROFILES%>" onClick='return checkfieldforlegalchars("document.editprofiles.<%=helper.TEXTFIELD_HARDTOKENPROFILESNAME%>","<%= ejbcawebbean.getText("ONLYCHARACTERS") %>")' value="<%= ejbcawebbean.getText("USESELECTEDASTEMPLATE") %>">
        </td>
      </tr>
      <tr> 
        <td width="5%">&nbsp; </td>
        <td width="95%">&nbsp;</td>
      </tr>
    </table>
  </form>
  <p align="center">&nbsp;</p>
  <p>&nbsp;</p>
</div>

