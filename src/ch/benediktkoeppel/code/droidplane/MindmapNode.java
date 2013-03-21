package ch.benediktkoeppel.code.droidplane;

import java.util.ArrayList;
import java.util.Locale;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;



/**
 * A MindMapNode is a special type of DOM Node. A DOM Node can be converted to a
 * MindMapNode if it has type ELEMENT, and tag "node".
 */
public class MindmapNode extends LinearLayout {
	
	/**
	 * the Text of the node (TEXT attribute).
	 */
	public String text;
	
	/**
	 * the name of the icon
	 */
	public String icon_name;
	
	/**
	 * the Android resource ID of the icon
	 */
	public int icon_res_id;
	
	/**
	 * whether the node is expandable, i.e. whether it has child nodes
	 */
	public boolean isExpandable;
	
	/**
	 * if the node has a LINK attribute, it will be stored in Uri link
	 */
	public Uri link;
	
	/**
	 * the XML DOM node from which this MindMapNode is derived
	 */
	private Node node;
	
	/**
	 * whether the node is selected or not, will be set after it was clicked by the user
	 */
	public boolean selected;	
	
	/**
	 * This constructor is only used to make graphical GUI layout tools happy. If used in running code, it will always throw a IllegalArgumentException.
	 * @deprecated
	 * @param context
	 */
	public MindmapNode(Context context) {
		super(context);
		if ( !isInEditMode() ) {
			throw new IllegalArgumentException(
					"The constructor public MindmapNode(Context context) may only be called by graphical layout tools, i.e. when View#isInEditMode() is true. In production, use the constructor public MindmapNode(Context context, Node node).");
		}
	}

	/**
	 * Creates a new MindMapNode from Node. The node needs to be of type ELEMENT and have tag "node". 
	 * Throws a {@link ClassCastException} if the Node can not be converted to a MindmapNode. 
	 * @param node
	 */
	public MindmapNode(Context context, Node node) {
		
		super(context);
		
		// convert the XML Node to a XML Element
		Element tmp_element;
		if ( isMindmapNode(node) ) {
			tmp_element = (Element)node;
		} else {
			throw new ClassCastException("Can not convert Node to MindmapNode");
		}
		
		// store the Node
		this.node = node;
			
		// extract the string (TEXT attribute) of the nodes
		// TODO: how do we handle rich text nodes?
		text = tmp_element.getAttribute("TEXT");
		
		String linkAttribute = tmp_element.getAttribute("LINK");
		if ( !linkAttribute.equals("") ) {
			link = Uri.parse(linkAttribute);
		}

		// extract icons
		// TODO: how do we handle multiple icons?
		ArrayList<String> icons = getIcons();
		String icon="";
		icon_res_id = 0;
		if ( icons.size() > 0 ) {
			icon = icons.get(0);
			icon_res_id = MainApplication.getStaticApplicationContext().getResources().getIdentifier("@drawable/" + icon, "id", MainApplication.getStaticApplicationContext().getPackageName());
		}

		// find out if it has sub nodes
		isExpandable = ( getNumChildMindmapNodes() > 0 );
		
		// load the layout from the XML file
		// TODO: somehow the height of the item is wrong
        MindmapNode.inflate(context, R.layout.mindmap_node_list_item, this);
        refreshView();
		
	}
	
	
	public void refreshView() {
		
		// the mindmap_node_list_item consists of a ImageView (icon), a TextView (node text), and another TextView ("+" button)
		ImageView iconView = (ImageView)findViewById(R.id.icon);
		iconView.setImageResource(icon_res_id);
		iconView.setContentDescription(icon_name);
		
		TextView textView = (TextView) findViewById(R.id.label);
		textView.setTextColor(getContext().getResources().getColor(android.R.color.primary_text_light));
		textView.setText(text);
		
		ImageView expandable = (ImageView) findViewById(R.id.expandable);
		if ( isExpandable ) {
			if ( getIsSelected() ) {
				expandable.setImageResource(R.drawable.minus_alt);
			} else {
				expandable.setImageResource(R.drawable.plus_alt);
			}
		}
		
		// if the node is selected and has child nodes, give it a special background
		if ( getIsSelected() && getNumChildMindmapNodes() > 0 ) {
			int backgroundColor;
			
			// menu bar: if we are at least at API 11, the Home button is kind of a back button in the app
	    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
	    		backgroundColor = getContext().getResources().getColor(android.R.color.holo_blue_bright);
	    	} else {
	    		backgroundColor = getContext().getResources().getColor(android.R.color.darker_gray);
	    	}
			
			setBackgroundColor(backgroundColor);	
		} else {
			setBackgroundColor(0);
		}	
		
		// set the layout parameter
		// TODO: this should not be necessary. The problem is that the inflate
		// (in the constructor) loads the XML as child of this LinearView, so
		// the MindmapNode-LinearView wraps the root LinearView from the
		// mindmap_node_list_item XML file.
		setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.WRAP_CONTENT));
		setGravity(Gravity.LEFT | Gravity.CENTER);
//		int paddingPx = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics());
//		setPadding(paddingPx, paddingPx, 0, paddingPx);
	}
	
	
	
	
	/**
	 * Returns the XML Node of which this MindmapNode was derived
	 * @return
	 */
	public Node getNode() {
		return this.node;
	}
	
	/**
	 * Selects or deselects this node
	 * @param selected
	 */
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	/**
	 * Returns whether this node is selected
	 */
	public boolean getIsSelected() {
		return this.selected;
	}
	
	/**
	 * Checks whether a given node can be converted to a Mindmap node, i.e.
	 * whether it has type ELEMENT_NODE and tag "node"
	 * @param node
	 * @return
	 */
	public static boolean isMindmapNode(Node node) {
		
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element) node;

			if (element.getTagName().equals("node")) {
				return true;
			}
		}
		return false;
	}


	/**
	 * Extracts the list of icons from a node and returns the names of the icons
	 * as ArrayList.
	 * 
	 * @return list of names of the icons
	 */
	private ArrayList<String> getIcons() {
		
		ArrayList<String> icons = new ArrayList<String>();
		
		NodeList childNodes = node.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			
			Node n = childNodes.item(i);
			if ( n.getNodeType() == Node.ELEMENT_NODE ) {
				Element e = (Element)n;
				
				if ( e.getTagName().equals("icon") && e.hasAttribute("BUILTIN") ) {
					icons.add(getDrawableNameFromMindmapIcon(e.getAttribute("BUILTIN")));
				}
			}
		}
		
		return icons;
	}
	
	/**
	 * Mindmap icons have names such as 'button-ok', but resources have to have
	 * names with pattern [a-z0-9_.]. This method translates the Mindmap icon
	 * names to Android resource names.
	 * 
	 * @param iconName the icon name as it is specified in the XML
	 * @return the name of the corresponding android resource icon
	 */
	private String getDrawableNameFromMindmapIcon(String iconName) {
		Locale locale = MainApplication.getStaticApplicationContext().getResources().getConfiguration().locale;
		String name = "icon_" + iconName.toLowerCase(locale).replaceAll("[^a-z0-9_.]", "_");
		name.replaceAll("_$", "");
		return name;
	}
	
	/**
	 * returns the number of child Mindmap nodes 
	 * @return
	 */
	public int getNumChildMindmapNodes() {
		
		int numMindmapNodes = 0;
		
		NodeList childNodes = node.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			
			Node n = childNodes.item(i);
			if ( isMindmapNode(n) ) {
				numMindmapNodes++;
			}
		}
		
		return numMindmapNodes;
	}
	
	/**
	 * The NodeColumn forwards the CreateContextMenu event to the appropriate
	 * MindmapNode, which can then generate the context menu as it likes. Note
	 * that the MindmapNode itself is not registered as the listener for such
	 * events per se, because the NodeColumn first has to decide for which
	 * MindmapNode the event applies.
	 * 
	 * TODO: is this really true? Or could we set a onCreateContextMenu listener
	 * for a ListItem?
	 * 
	 * @param menu
	 * @param v
	 * @param menuInfo
	 */
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		
		MainApplication.getMainActivityInstance().setNextContextMenuMindmapNode(this);
		
		
		// build the menu
		menu.setHeaderTitle(text);
		menu.setHeaderIcon(icon_res_id);
		
		// add a submenu showing all icons
		// Context menus do not support icons.
//		SubMenu iconMenu = menu.addSubMenu("All icons");
//		ArrayList<String> icons = getIcons();
//		for (int i = 0; i < icons.size(); i++) {
//			String icon = icons.get(i);
//			MenuItem iconMenuItem = iconMenu.add(icon);
//			int iconMenuItemResId = MainApplication.getStaticApplicationContext().getResources().getIdentifier("@drawable/" + icon, "id", MainApplication.getStaticApplicationContext().getPackageName());
//			iconMenuItem.setIcon(MainApplication.getStaticApplicationContext().getResources().getDrawable(iconMenuItemResId));
//		}

		// allow copying the node text
		menu.add(0, R.id.contextcopy, 0, R.string.copynodetext);

		// TODO: add menu to open link, if the node has a hyperlink
		if ( link != null ) {
			menu.add(0, R.id.contextopenlink, 0, R.string.openlink);
		}
		
		
		
	}
}
