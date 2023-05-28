<script lang="ts">
    import { page} from "$app/stores";

    let organization = $page.params.organization ?? "";
    let gdqId = $page.params.gdq_id ?? "";
    let horaroId = $page.params.horaro_id ?? "";

</script>

<h1>Suggest a VOD</h1>

<form>
    <label for="url">URL</label>
    <input type="text" id="url" name="url" placeholder="https://www.youtube.com/watch?v=dQw4w9WgXcQ">

    <label for="organisation">Organisation</label>
    <input type="text" id="organisation" name="organisation" value="{ organization }">

    <label for="gdqId" >GDQ ID</label>
    <input type="text" id="gdqId" name="gdqId" value="{ gdqId }">

    <label for="horaroId" >Horaro ID</label>
    <input type="text" id="horaroId" name="horaroId" value="{ horaroId }">
</form>