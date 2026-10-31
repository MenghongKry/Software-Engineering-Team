<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c"    uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>New product</title>
  <link rel="stylesheet" href="/css/site.css">
</head>
<body>
<h1>New product</h1>

<c:if test="${not empty error}">
  <p class="error"><c:out value="${error}"/></p>
</c:if>

<%-- form:input / form:errors HTML-escape their output automatically --%>
<form:form method="post" action="/products" modelAttribute="productForm">
  <p>
    <label>Name</label>
    <form:input path="name"/>
    <form:errors path="name" cssClass="error"/>
  </p>
  <p>
    <label>Price</label>
    <form:input path="price"/>
    <form:errors path="price" cssClass="error"/>
  </p>
  <p>
    <label>Stock</label>
    <form:input path="stock"/>
    <form:errors path="stock" cssClass="error"/>
  </p>
  <button type="submit">Save</button>
  <a href="/products">Cancel</a>
</form:form>
</body>
</html>
