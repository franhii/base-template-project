import React, {useEffect, useState} from "react";
import axios from "axios";

export default function ItemList(){
  const [products, setProducts] = useState([]);
  useEffect(()=>{ axios.get('/api/items/products').then(r=>setProducts(r.data)); },[]);
  return (
    <div>
      <h2>Productos</h2>
      <div style={{display:'flex',gap:10,flexWrap:'wrap'}}>
        {products.map(p=>(
          <div key={p.id} style={{border:'1px solid #ddd',padding:10,width:200}}>
            <h4>{p.name}</h4>
            <div>{p.description}</div>
            <div>Precio: {p.price}</div>
          </div>
        ))}
      </div>
    </div>
  )
}
